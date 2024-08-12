package com.ironclad.clangoals.components.tracking;

import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.PluginStateChanged;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.components.service.dto.TrackingConfig;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.util.BatchQueue;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@RequiredArgsConstructor
public abstract class AbstractTrackingComponent<Q> implements Component
{
	private final TrackingConfig.Type type;
	protected final ApiService api;
	protected final EventBus eventBus;
	protected final ScheduledExecutorService executor;
	@Getter(AccessLevel.PROTECTED)
	private BatchQueue<Q> queue;
	@Getter(AccessLevel.PROTECTED)
	private PluginState state;

	@Override
	public final void onStartUp(PluginState state)
	{
		this.state = state;
		rebuildQueue(state.getRemoteConfig());
		eventBus.register(this);
		onComponentStart(state);
	}

	@Override
	public final void onShutDown(PluginState state)
	{
		queue.shutdown();
		eventBus.unregister(this);
		onComponentStop(state);
	}

	protected void onComponentStart(PluginState state)
	{

	}

	protected void onComponentStop(PluginState state)
	{

	}

	protected abstract void onFlush(List<Q> items);

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return state.isAuthenticated()
			&& !state.getRemoteConfig().isMaintenance()
			&& state.getRemoteConfig().getBatchConfigs().get(type).isEnabled();
	}

	@Subscribe
	private void onPluginStateChanged(PluginStateChanged e)
	{
		state = e.getCurrent();
	}

	@Subscribe
	private void onRemoteConfigChanged(RemoteConfigChanged e)
	{
		rebuildQueue(e.getCurrent());
	}

	private void rebuildQueue(RemoteConfig config)
	{
		TrackingConfig bconf = config.getBatchConfigs().get(type);
		if (queue != null)
		{
			queue.shutdown();
		}
		queue = new BatchQueue<>(bconf.getSize(), bconf.getInterval(), this::onFlush);
		queue.start(executor);
	}

	protected boolean blockTracking()
	{
		return !getState().isInGame() || !getState().isInEnabledWorld() || !getState().isInClan();
	}
}
