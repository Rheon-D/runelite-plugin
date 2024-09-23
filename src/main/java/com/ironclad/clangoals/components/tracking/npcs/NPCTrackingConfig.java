package com.ironclad.clangoals.components.tracking.npcs;

import com.google.gson.annotations.SerializedName;
import com.ironclad.clangoals.components.service.config.dto.QueueConfig;
import com.ironclad.clangoals.components.tracking.TrackingConfig;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

@Data
@Builder(access = AccessLevel.PACKAGE)
public class NPCTrackingConfig implements TrackingConfig<NPCTrackingConfig>
{
	boolean enabled;
	@SerializedName("batch_config")
	QueueConfig queueConfig;
	@SerializedName("message_matchers")
	List<MessageMatcher> messageMatchers;
	@SerializedName("mappings")
	List<Mapping> mappings;
	@SerializedName("raid_whitelist")
	Set<String> whitelistedRaidNpcs;
	@SerializedName("loot_only_npcs")
	Set<Integer> lootOnlyNpcs;

	@Override
	public void update(NPCTrackingConfig other)
	{
		this.enabled = other.enabled;
		this.queueConfig = other.queueConfig;
		this.messageMatchers = other.messageMatchers;
		this.mappings = other.mappings;
		this.whitelistedRaidNpcs = other.whitelistedRaidNpcs;
		this.lootOnlyNpcs = other.lootOnlyNpcs;
	}

	@Override
	public boolean isEnabled()
	{
		return this.enabled;
	}

	public static final NPCTrackingConfig DEFAULT;
	public static final Predicate<NPCTrackingConfig> VALIDATOR;

	static
	{
		DEFAULT = getEmpty();

		VALIDATOR = config -> {
			if (ObjectUtils.allNotNull(config, config.queueConfig, config.mappings, config.messageMatchers, config.whitelistedRaidNpcs, config.lootOnlyNpcs))
			{
				boolean queueValid = QueueConfig.VALIDATOR.test(config.queueConfig);
				boolean mappingsValid = config.mappings.stream().allMatch(Mapping.VALIDATOR);
				boolean matchersValid = config.messageMatchers.stream().allMatch(MessageMatcher.VALIDATOR);
				return queueValid && mappingsValid && matchersValid;
			}
			return false;
		};
	}

	public static NPCTrackingConfig getEmpty()
	{
		return NPCTrackingConfig.builder()
			.enabled(false)
			.queueConfig(QueueConfig.builder().size(100).interval(600).build())
			.messageMatchers(List.of())
			.mappings(List.of())
			.whitelistedRaidNpcs(Set.of())
			.lootOnlyNpcs(Set.of())
			.build();
	}
}
