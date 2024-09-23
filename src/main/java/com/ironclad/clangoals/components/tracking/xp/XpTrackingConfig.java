package com.ironclad.clangoals.components.tracking.xp;

import com.google.gson.annotations.SerializedName;
import com.ironclad.clangoals.components.service.config.dto.QueueConfig;
import com.ironclad.clangoals.components.tracking.TrackingConfig;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PACKAGE)
public class XpTrackingConfig implements TrackingConfig<XpTrackingConfig>
{
	boolean enabled;
	@SerializedName("batch_config")
	QueueConfig queueConfig;

	@Override
	public void update(XpTrackingConfig other)
	{
		this.enabled = other.enabled;
		this.queueConfig.update(other.queueConfig);
	}

	@Override
	public boolean isEnabled()
	{
		return this.enabled;
	}

	public static final XpTrackingConfig DEFAULT = getEmpty();
	public static final Predicate<XpTrackingConfig> VALIDATOR = config -> config != null && QueueConfig.VALIDATOR.test(config.queueConfig);

	public static XpTrackingConfig getEmpty(){
		return XpTrackingConfig.builder()
			.enabled(false)
			.queueConfig(QueueConfig.builder()
				.size(100)
				.interval(600)
				.build())
			.build();
	}
}
