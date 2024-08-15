package com.ironclad.clangoals.components.tracking.npcs;


import static com.ironclad.clangoals.components.tracking.npcs.NPCTrackingComponent.MISSING_DELAY_TICKS;
import java.time.Instant;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Hitsplat;
import net.runelite.client.util.RSTimeUnit;

@Data
@RequiredArgsConstructor(onConstructor_ = @NonNull)
final class TrackedNpc
{
	private final int index;
	private final int id;
	private final int health;
	private final String name;
	private Instant missing;
	private int damageMe = 0;
	private int damageOther = 0;

	public void setMissing(boolean missing){
		this.missing = missing? Instant.now().plus(MISSING_DELAY_TICKS): null;
	}

	public boolean isMissing(){
		return missing != null;
	}

	public boolean isTimedOut(){
		return Instant.now().isAfter(missing);
	}

	public void applyHitsplat(@NonNull Hitsplat hitsplat)
	{
		if (damageOther == Integer.MAX_VALUE)
		{
			return; //We already know we can't get the kill.
		}

		if (hitsplat.isMine())
		{
			damageMe += hitsplat.getAmount();
		}
		else if (hitsplat.isOthers())
		{
			damageOther += hitsplat.getAmount();
		}
		else if (hitsplat.getHitsplatType() == 1)//That blocky thing
		{
			damageOther = Integer.MAX_VALUE; //Assume we can't get the kill.
		}
	}

	public boolean isMyKill()
	{
		return damageMe > damageOther
			&& damageMe >= health / 2;
	}
}
