package com.ironclad.clangoals.components.tracking.loot;

import com.google.gson.annotations.SerializedName;
import java.util.function.Predicate;
import joptsimple.internal.Strings;
import lombok.NonNull;
import lombok.Value;
import net.runelite.http.api.loottracker.LootRecordType;

/**
 * Represents a trackable loot received event or action, hence the shit name.
 * This could include events such as Barrows, Chambers of Xeric, etc.
 * Where we aren't necessarily tracking a specific item, but the action of receiving loot.
 */
@Value
class Mapping
{
	/**
	 * The event per {@link net.runelite.client.plugins.loottracker.LootTrackerPlugin}
	 */
	@NonNull String event;
	/**
	 * The name that will be shown to users through ui
	 */
	@SerializedName("display_name")
	@NonNull String displayName;
	/**
	 * Item id for the icon in ui.
	 */
	@SerializedName("icon_id")
	int iconID;
	LootRecordType type;
	int id;

	static final Predicate<Mapping> VALIDATOR = m -> !Strings.isNullOrEmpty(m.event) && !Strings.isNullOrEmpty(m.displayName) && m.type != null && m.iconID > 0;
}
