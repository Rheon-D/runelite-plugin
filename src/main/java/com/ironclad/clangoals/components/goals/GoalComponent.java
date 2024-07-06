package com.ironclad.clangoals.components.goals;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.PluginStateChanged;
import com.ironclad.clangoals.components.service.dto.XPGoal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GoalComponent implements Component
{

	private final EventBus eventBus;
	private final ClientToolbar clientToolbar;
	private final GoalPanel panel;
	private final ApiService api;
	private ScheduledExecutorService executor;
	private NavigationButton navigationButton;

	@Override
	public void onStartUp(PluginState state)
	{
		if (navigationButton == null)
		{
			navigationButton = NavigationButton.builder()
				.tooltip("Ironclad Clan Goals")
				.icon(GoalPanel.ICON)
				.priority(999)
				.panel(panel)
				.build();
		}
		executor = Executors.newSingleThreadScheduledExecutor();
		//executor.scheduleAtFixedRate(this::updatePanel, 0, 1, TimeUnit.SECONDS);
		clientToolbar.addNavigation(navigationButton);
		eventBus.register(this);
	}

	@Override
	public void onShutDown(PluginState state)
	{
		clientToolbar.removeNavigation(navigationButton);
		executor.shutdown();
		eventBus.unregister(this);
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return true;
	}

	private void updatePanel()
	{
		CompletableFuture<List<XPGoal>> future = api.getGoals();
		//future.thenAccept(panel::updateGoals).exceptionally(panel::failedUpdate);
	}

	@Subscribe
	private void onPluginStateChanged(PluginStateChanged e){

	}
}
