package com.ironclad.clangoals.components.service.config.dto;

import com.google.gson.annotations.SerializedName;
import com.google.inject.Singleton;
import com.ironclad.clangoals.components.service.config.Updatable;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingConfig;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingConfig;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingConfig;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
@AllArgsConstructor
public class RemoteConfig implements Updatable<RemoteConfig>
{
	@SerializedName("last_updated")
	Instant lastUpdated;
	@SerializedName("refresh_interval")
	int refreshInterval;
	@SerializedName("is_maintenance")
	boolean maintenance;
	@SerializedName("xp_tracking")
	XpTrackingConfig xpTrackingConfig;
	@SerializedName("item_tracking")
	ItemTrackingConfig itemTrackingConfig;
	@SerializedName("npc_tracking")
	NPCTrackingConfig npcTrackingConfig;

	public void update(RemoteConfig other)
	{
		this.lastUpdated = other.lastUpdated;
		this.refreshInterval = other.refreshInterval;
		this.maintenance = other.maintenance;
		this.npcTrackingConfig.update(other.npcTrackingConfig);
		this.itemTrackingConfig.update(other.itemTrackingConfig);
		this.xpTrackingConfig.update(other.xpTrackingConfig);
	}
}
