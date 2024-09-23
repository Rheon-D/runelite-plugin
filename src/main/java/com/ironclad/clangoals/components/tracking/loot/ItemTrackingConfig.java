package com.ironclad.clangoals.components.tracking.loot;

import com.google.gson.annotations.SerializedName;
import com.ironclad.clangoals.components.service.config.dto.QueueConfig;
import com.ironclad.clangoals.components.tracking.TrackingConfig;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PACKAGE)
public class ItemTrackingConfig implements TrackingConfig<ItemTrackingConfig>
{
	boolean enabled;
	@SerializedName("batch_config")
	QueueConfig queueConfig;
	@SerializedName("loot_events")
	Set<Mapping> lootEvents;

	@Override
	public void update(ItemTrackingConfig other)
	{
		this.enabled = other.enabled;
		this.lootEvents = other.lootEvents;
		this.queueConfig.update(other.queueConfig);
	}

	@Override
	public boolean isEnabled()
	{
		return this.enabled;
	}

	public static final ItemTrackingConfig DEFAULT;
	public static final Predicate<ItemTrackingConfig> VALIDATOR;

	static {
		DEFAULT = getEmpty();
		VALIDATOR = config -> {
			if(config != null && config.lootEvents != null){
				boolean queueValid = QueueConfig.VALIDATOR.test(config.queueConfig);
				boolean mappingsValid = config.lootEvents.stream().allMatch(Mapping.VALIDATOR);
				return queueValid && mappingsValid;
			}
			return false;
		};
	}

	public static ItemTrackingConfig getEmpty(){
		return ItemTrackingConfig.builder()
			.enabled(false)
			.queueConfig(QueueConfig.builder()
				.size(100)
				.interval(150)
				.build())
			.lootEvents(Set.of())
			.build();
	}
}
