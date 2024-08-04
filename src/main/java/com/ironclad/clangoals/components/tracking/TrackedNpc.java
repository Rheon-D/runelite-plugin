package com.ironclad.clangoals.components.tracking;

import java.time.Instant;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Hitsplat;

@Getter
@RequiredArgsConstructor(onConstructor_ = @NonNull)
class TrackedNpc
{
	private Instant lastSeen;
	private final int id;
	private final int index;
	private final String name;
	private int damageMe = 0;
	private int damageOther = 0;

	public void setMissing(boolean missing){
		if(missing){
			lastSeen = Instant.now();
		}else{
			lastSeen = null;
		}
	}

	public Optional<Instant> getLastSeen(){
		return Optional.ofNullable(lastSeen);
	}

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
