package com.ironclad.clangoals.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Region
{
	BARBARIAN_ASSAULT(ImmutableSet.of()),//TODO: Find the correct region
	CASTLE_WARS(ImmutableSet.of(9520)),
	CHAMBERS_OF_XERIC(ImmutableSet.of(12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401)),
	COLOSSEUM(ImmutableSet.of(7216)),
	FIGHT_CAVES(ImmutableSet.of()),//TODO: Find the correct region
	FIGHT_PITS(ImmutableSet.of()),//TODO: Find the correct region
	GAUNTLET(ImmutableSet.of()), //TODO: Find the correct region
	INFERNO(ImmutableSet.of()),//TODO: Find the correct region
	LAST_MAN_STANDING(ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432)),
	NIGHTMARE_ZONE(ImmutableSet.of(9033)),
	PEST_CONTROL(ImmutableSet.of()),//TODO: Find the correct region
	PVP_ARENA(ImmutableSet.of(13362)),
	RAT_PITS(ImmutableSet.of(10646, 7753, 11599, 11926)),
	SOUL_WARS(ImmutableSet.of(8493, 8749, 9005)),
	TEMPOROSS(ImmutableSet.of(12078)),
	THEATRE_OF_BLOOD(ImmutableSet.of(12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, 13379)),
	TOMES_OF_AMASCUT(ImmutableSet.of(14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15698, 15700, 15696)),
	TROUBLE_BREWING(ImmutableSet.of(15150)),
	VOLCANIC_MINE(ImmutableSet.of(15263, 15262)),
	WINTERTODT(ImmutableSet.of(6462)),
	ZALCANO(ImmutableSet.of(13250)),
	;

	private final Set<Integer> regions;

	public boolean inRegion(int region)
	{
		return this.regions.contains(region);
	}

	public static Set<Integer> combine(Region... regions)
	{
		ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
		for (Region region : regions)
		{
			builder.addAll(region.regions);
		}
		return builder.build();
	}
}
