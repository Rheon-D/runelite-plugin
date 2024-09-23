package com.ironclad.clangoals.component;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.PluginStateChanged;
import com.ironclad.clangoals.components.service.ServiceComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.client.util.GameEventManager;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ComponentManager
{
	private final EventBus eventBus;
	private final IroncladClanGoalsConfig config;
	private final ServiceComponent pluginStateTracker;
	private final GameEventManager gameEventManager;
	private final Set<Component> components;
	private final Map<Component, Boolean> states = new HashMap<>();
	//public static final boolean stolenFromLlemonDuck = true;
	public void onPluginStart()
	{
		this.eventBus.register(this);
		this.components.forEach(c -> this.states.put(c, false));
		revalidate();
	}

	public void onPluginStop()
	{
		this.eventBus.unregister(this);
		this.components.stream()
			.filter(this.states::get)
			.forEach(c -> tryShutDown(c, this.pluginStateTracker.getState()));
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged e)
	{
		if (!IroncladClanGoalsConfig.CONFIG_GROUP.equals(e.getGroup()))
		{
			return;
		}
		revalidate();
	}

	@Subscribe
	private void onPluginStateChanged(PluginStateChanged e)
	{
		revalidate();
	}

	private void revalidate()
	{
		PluginState state = this.pluginStateTracker.getState();
		this.components.forEach(c ->
		{
			boolean shouldBeEnabled = c.isEnabled(this.config, state);
			boolean isEnabled = this.states.get(c);
			if (shouldBeEnabled == isEnabled)
			{
				return;
			}
			if (shouldBeEnabled)
			{
				tryStartUp(c, state);
			}
			else
			{
				tryShutDown(c, state);
			}
		});
	}

	private void tryStartUp(Component c, PluginState pluginState)
	{
		if(this.states.get(c)) return;

		log.debug("Starting up component: {}", c.getClass().getSimpleName());

		try
		{
			c.onStartUp(pluginState);
			this.gameEventManager.simulateGameEvents(c);
			this.states.put(c, true);
		}
		catch (Exception e)
		{
			log.error("Error starting up component: {}", c.getClass().getSimpleName(), e);
		}
	}

	private void tryShutDown(Component c, PluginState state)
	{
		if (!this.states.get(c))
		{
			return;
		}

		log.debug("Shutting down component: {}", c.getClass().getSimpleName());
		try
		{
			c.onShutDown(state);
		}
		catch (Exception e)
		{
			log.error("Error shutting down component: {}", c.getClass().getSimpleName(), e);
		}
		finally
		{
			this.states.put(c, false);
		}
	}
}
