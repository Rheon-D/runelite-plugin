package com.ironclad.clangoals;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.ironclad.clangoals.component.ComponentManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "IronClad Clan Goals",
	description = "A tracker to allow participants to contribute towards IronClad clan goals",
	tags = {"IronClad", "clan", "goals"}
)
public class IroncladClanGoalsPlugin extends Plugin
{
	private ComponentManager componentManager;
	@Override
	public void configure(Binder binder)
	{
		binder.install(new IroncladModule());
	}

	@Override
	protected void startUp()
	{
		if (componentManager == null)
		{
			componentManager = injector.getInstance(ComponentManager.class);
		}
		componentManager.onPluginStart();
	}

	@Override
	protected void shutDown()
	{
		componentManager.onPluginStop();
	}
}
