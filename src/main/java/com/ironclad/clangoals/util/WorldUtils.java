package com.ironclad.clangoals.util;

import java.util.EnumSet;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;

import java.util.Set;
import net.runelite.api.WorldType;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@UtilityClass
public class WorldUtils
{
	private static final EnumSet<WorldType> DISABLED_WORLDS = EnumSet.of(
		WorldType.DEADMAN,
		WorldType.BETA_WORLD,
		WorldType.NOSAVE_MODE,
		WorldType.TOURNAMENT_WORLD,
		WorldType.FRESH_START_WORLD,
		WorldType.LAST_MAN_STANDING,
		WorldType.SEASONAL,
		WorldType.QUEST_SPEEDRUNNING,
		WorldType.PVP_ARENA
	);

	public boolean inRegion(@NonNull Client client, @NonNull EnumSet<Region> regions)
	{
		int region = getPlayerRegion(client);
		return inRegion(region, regions);
	}

	public boolean inRegion(int region, @NonNull EnumSet<Region> regions)
	{
		for (Region r : regions)
		{
			if (r.inRegion(region))
			{
				return true;
			}
		}
		return false;
	}

	public boolean inRegion(@NonNull Client client, @NonNull Region... regions)
	{
		return inRegion(getPlayerRegion(client), regions);
	}

	public boolean inRegion(int region, @NonNull Region... regions)
	{
		for (Region r : regions)
		{
			if (r.inRegion(region))
			{
				return true;
			}
		}
		return false;
	}

	public int getPlayerRegion(@NonNull Client client)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		return lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();
	}

	public boolean isPlayerWithinMapRegion(Client client, Set<Integer> definedMapRegions)
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

	public boolean isDisabledWorldType(@NonNull EnumSet<WorldType> worldTypes)
	{
		for (WorldType worldType : worldTypes)
		{
			if (DISABLED_WORLDS.contains(worldType))
			{
				return true;
			}
		}
		return false;
	}
}
