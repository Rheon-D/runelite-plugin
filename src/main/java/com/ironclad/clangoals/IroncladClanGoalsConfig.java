package com.ironclad.clangoals;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(IroncladClanGoalsConfig.CONFIG_GROUP)
public interface IroncladClanGoalsConfig extends Config
{
	String CONFIG_GROUP = "ironcladclangoals";
	String API_KEY = "apiKey";
	String EVENT_PASSWORD = "eventPassword";
	String EVENT_OVERLAY_KEYBIND = "eventOverlayKeybind";

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

	@ConfigSection(
		name = "Event Overlay",
		description = "Event overlay configuration",
		position = 3,
		closedByDefault = true
	)
	String eventOverlay = "eventOverlay";

	@ConfigItem(
		keyName = API_KEY,
		name = "API Key",
		description = "The API key used to authenticate with IronClad's API",
		position = 0
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
		position = 0
	)
	default boolean statusMessages()
	{
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

	@ConfigItem(
		keyName = "enableEventOverlay",
		name = "Password Overlay",
		description = "Enable the event password overlay",
		position = 1,
		section = eventOverlay
	)
	default boolean eventOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = EVENT_PASSWORD,
		name = "Event Password",
		description = "The password for the event",
		position = 2,
		section = eventOverlay
	)
	default String eventPassword()
	{
		return "";
	}

	@ConfigItem(
		keyName = "eventPasswordColor",
		name = "Password Color",
		description = "Color of the password in the overlay",
		position = 3,
		section = eventOverlay
	)
	default Color eventPasswordColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "eventTimestampColor",
		name = "Timestamp Color",
		description = "Color of the timestamp in the overlay",
		position = 4,
		section = eventOverlay
	)
	default Color eventTimestampColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = EVENT_OVERLAY_KEYBIND,
		name = "Overlay Keybind",
		description = "The keybind to toggle the overlay",
		position = 5,
		section = eventOverlay
	)
	default Keybind eventOverlayKeybind()
	{
		return Keybind.NOT_SET;
	}
}
