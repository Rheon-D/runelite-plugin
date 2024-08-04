package com.ironclad.clangoals.components.tracking;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Hitsplat;

@Getter
@RequiredArgsConstructor(onConstructor_ = @NonNull)
class TrackedNpc
{
	private final int id;
	private final int index;
	private final String name;
	private int damageMe = 0;
	private int damageOther = 0;

	public void applyHitsplat(@NonNull Hitsplat hitsplat)
	{
		if (hitsplat.isMine())
		{
			damageMe += hitsplat.getAmount();
		}
		else if (hitsplat.isOthers())
		{
			damageOther += hitsplat.getAmount();
		}
		//Assume other hitsplats are irr-elephant
	}

	public boolean isMyKill()
	{
		return damageMe > damageOther;
	}
}
