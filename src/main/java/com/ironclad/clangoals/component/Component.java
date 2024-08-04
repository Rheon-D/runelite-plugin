package com.ironclad.clangoals.component;

import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.PluginState;

public interface Component
{
	default boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return state.isAuthenticated() && !state.getRemoteConfig().isMaintenance();
	}

	void onStartUp(PluginState state);

	void onShutDown(PluginState state);
}
