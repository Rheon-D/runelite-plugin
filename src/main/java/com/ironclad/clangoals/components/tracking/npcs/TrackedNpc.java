package com.ironclad.clangoals.components.tracking.npcs;


import static com.ironclad.clangoals.components.tracking.npcs.NPCTrackingComponent.MISSING_DELAY_TICKS;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.runelite.api.Hitsplat;

@Data
@Builder
final class TrackedNpc
{
	final int index;
	int id;
	@NonNull String name;
	Instant missing;
	int damageMe;
	int damageOther;
	final CreditMethod creditMethod;

	public void setMissing(boolean missing){
		this.missing = missing? Instant.now().plus(MISSING_DELAY_TICKS): null;
	}

	public boolean isMissing(){
		return this.missing != null;
	}

	public boolean isTimedOut(){
		return Instant.now().isAfter(this.missing);
	}

	public void applyHitsplat(@NonNull Hitsplat hitsplat)
	{
		if (this.damageOther == Integer.MAX_VALUE)
		{
			return; //We already know we can't get the kill.
		}

		if (hitsplat.isMine())
		{
			this.damageMe += hitsplat.getAmount();
		}
		else if (hitsplat.isOthers())
		{
			this.damageOther += hitsplat.getAmount();
		}
	}

	public boolean creditMe()
	{
		switch(this.creditMethod)
		{
			case MOST_DAMAGE:
				return this.damageMe > this.damageOther;
			case ONLY_DAMAGE:
				return this.damageMe > 0 && this.damageOther == 0;
			case CONTRIBUTED:
				return this.damageMe > 0;
			case ALWAYS:
			default:
				return true; //HUH
		}
	}

	enum CreditMethod
	{
		MOST_DAMAGE,
		ONLY_DAMAGE,
		CONTRIBUTED,
		ALWAYS
	}
}
