package com.ironclad.clangoals.components.tracking;

import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.PluginStateChanged;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.components.service.config.dto.QueueConfig;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.util.BatchQueue;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@RequiredArgsConstructor
public abstract class AbstractTrackingComponent<Q, C extends TrackingConfig<?>> implements Component
{
	@NonNull
	protected final ApiService api;
	@NonNull
	protected final EventBus eventBus;
	@NonNull
	protected final ScheduledExecutorService executor;
	@NonNull
	protected final RemoteConfig rConf;
	@Getter(AccessLevel.PROTECTED)
	private BatchQueue<Q> queue;
	@Getter(AccessLevel.PROTECTED)
	private PluginState state;

	@Override
	public final void onStartUp(PluginState state)
	{
		this.state = state;
		rebuildQueue();
		this.eventBus.register(this);
		onComponentStart(state);
	}

	@Override
	public final void onShutDown(PluginState state)
	{
		this.queue.shutdown();
		this.eventBus.unregister(this);
		onComponentStop(state);
	}

	protected void onComponentStart(PluginState state)
	{

	}

	protected void onComponentStop(PluginState state)
	{

	}

	protected abstract void onFlush(List<Q> items);

	protected abstract boolean componentEnabled(IroncladClanGoalsConfig config);

	protected abstract void rebuild(C config);

	protected abstract C getConfig();

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig pluginConfig, PluginState state)
	{
		return state.isAuthenticated()
			&& !this.rConf.isMaintenance()
			&& getConfig().isEnabled()
			&& componentEnabled(pluginConfig);
	}

	@Subscribe
	private void onPluginStateChanged(PluginStateChanged e)
	{
		this.state = e.getCurrent();
	}

	@Subscribe
	private void onRemoteConfigChanged(RemoteConfigChanged e)
	{
		rebuildQueue();
		rebuild(getConfig());
	}

	private void rebuildQueue()
	{
		QueueConfig qConf = getConfig().getQueueConfig();
		if (this.queue != null)
		{
			this.queue.shutdown();
		}
		this.queue = new BatchQueue<>(qConf.getSize(), qConf.getInterval(), this::onFlush);
		this.queue.start(this.executor);
	}

	protected boolean blockTracking()
	{
		return !getState().isInGame() || !getState().isInEnabledWorld() || !getState().isInClan();
	}
}
