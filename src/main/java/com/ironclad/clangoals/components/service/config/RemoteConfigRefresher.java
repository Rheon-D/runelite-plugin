package com.ironclad.clangoals.components.service.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.status.StatusMessage;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RemoteConfigRefresher implements Component
{
	private final EventBus eventBus;
	private final ScheduledExecutorService executor;
	private final RemoteConfigLoader remoteConfigService;
	private final RemoteConfig rConf;
	private final Lock lock = new ReentrantLock();
	private Instant nextRefreshAt = nextRefreshTime();
	private int refreshInterval;
	private ScheduledFuture<?> scheduledFuture;

	@Override
	public void onStartUp(PluginState state)
	{
		this.refreshInterval = this.rConf.getRefreshInterval();
		this.eventBus.register(this);
		setSchedule();
	}

	@Override
	public void onShutDown(PluginState state)
	{
		this.eventBus.unregister(this);
		stopSchedule();
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return state.isAuthenticated();
	}


	private Instant nextRefreshTime()
	{
		return Instant.now().plus(this.refreshInterval, ChronoUnit.MINUTES);
	}

	private void refresh()
	{
		this.lock.lock();
		try
		{
			if (Instant.now().isAfter(this.nextRefreshAt))
			{
				this.executor.submit(this.remoteConfigService::fetchConfiguration);
				this.nextRefreshAt = nextRefreshTime();
			}
		}
		finally
		{
			this.lock.unlock();
		}
	}

	private void setSchedule()
	{
		if (this.scheduledFuture != null)
		{
			return;
		}
		long start = Math.max(0, this.nextRefreshAt.toEpochMilli() - Instant.now().toEpochMilli());
		this.scheduledFuture = this.executor.scheduleWithFixedDelay(this::refresh, start, this.refreshInterval, TimeUnit.MINUTES);
	}

	private void stopSchedule()
	{
		if (this.scheduledFuture == null)
		{
			return;
		}
		this.scheduledFuture.cancel(true);
		this.scheduledFuture = null;
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
				stopSchedule();
				break;
			case LOGGING_IN:
				setSchedule();
				break;
		}
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted e)
	{
		if (e.getCommand().equalsIgnoreCase("icrconf"))
		{
			this.lock.lock();
			try
			{
				stopSchedule();
				this.executor.submit(this.remoteConfigService::fetchConfiguration);
				this.nextRefreshAt = nextRefreshTime();
				setSchedule();
				this.eventBus.post(StatusMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.value("Remote configuration reloaded.")
						.build());
			}
			finally
			{
				this.lock.unlock();
			}
		}
	}
}
