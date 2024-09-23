package com.ironclad.clangoals;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(IroncladClanGoalsConfig.CONFIG_GROUP)
public interface IroncladClanGoalsConfig extends Config
{
	String CONFIG_GROUP = "ironcladclangoals";
	String API_KEY = "apiKey";

	@ConfigSection(
		name = "Messaging",
		description = "Configuration for plugin messaging.",
		position = 1,
		closedByDefault = true
	)
	String messaging = "messaging";

	@ConfigSection(
		name = "Tracking",
		description = "Configuration for tracking.",
		position = 2,
		closedByDefault = true
	)
	String tracking = "tracking";

	@ConfigItem(
		keyName = API_KEY,
		name = "API Key",
		description = "The API key used to authenticate with IronClad's API",
		position =0
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "statusMessages",
		name = "Enable status messages",
		description = "Allow status messages from the plugin into the Clan Chat",
		section = messaging,
		position =0
	)
	default boolean statusMessages(){
		return true;
	}

	@ConfigItem(
		keyName = "enableNpcTracking",
		name = "NPC Tracking",
		description = "Enable tracking of NPC's",
		position = 2,
		section = tracking
	)
	default boolean enableNpcTracking()
	{
		return true;
	}
	@ConfigItem(
		keyName = "enableItemTracking",
		name = "Loot Tracking",
		description = "Enable tracking of loot drops and events.",
		position = 3,
		section = tracking
	)
	default boolean enableItemTracking()
	{
		return true;
	}
	@ConfigItem(
		keyName = "enableXpTracking",
		name = "XP Tracking",
		description = "Enable tracking of XP gains.",
		position = 4,
		section = tracking
	)
	default boolean enableXpTracking()
	{
		return true;
	}


}
