package com.ironclad.clangoals.util;

import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import net.runelite.api.Client;

import java.util.Set;
import net.runelite.api.WorldType;

public class WorldUtils
{
	private static final EnumSet<WorldType> DISABLED_WORLDS = EnumSet.of(WorldType.DEADMAN,
		WorldType.BETA_WORLD,
		WorldType.NOSAVE_MODE,
		WorldType.TOURNAMENT_WORLD,
		WorldType.FRESH_START_WORLD,
		WorldType.SEASONAL,
		WorldType.QUEST_SPEEDRUNNING
	);

	// @see https://github.com/runelite/runelite/blob/ab0ddd9ea48efc707be1352ec4dacb7cc93cc6fa/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/LootTrackerPlugin.java#L232
	public static final Set<Integer> LAST_MAN_STANDING_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);

	// @see https://github.com/runelite/runelite/blob/ab0ddd9ea48efc707be1352ec4dacb7cc93cc6fa/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/LootTrackerPlugin.java#L271
	public static final Set<Integer> SOUL_WARS_REGIONS = ImmutableSet.of(8493, 8749, 9005);

	public static boolean isPlayerWithinMapRegion(Client client, Set<Integer> definedMapRegions)
	{
		final int[] mapRegions = client.getMapRegions();

		for (int region : mapRegions)
		{
			if (definedMapRegions.contains(region))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if the world type is disabled.
	 * <p>
	 * but rather a list of world types that are enabled for a specific goal.
	 *
	 * @param worldTypes The {@link WorldType}s to check
	 * @return true if the world type is disabled or if the worldTypes is null
	 */
	public static boolean isDisabledWorldType(EnumSet<WorldType> worldTypes)
	{
		return worldTypes == null || worldTypes.stream().anyMatch(DISABLED_WORLDS::contains);
	}
}
