package com.ironclad.clangoals.util.predicate;

import com.ironclad.clangoals.components.service.dto.TrackingConfig;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import net.runelite.client.util.RSTimeUnit;

@RequiredArgsConstructor
public class ValidConfig implements Predicate<RemoteConfig>
{
	private static final Predicate<TrackingConfig> VALID_BATCH;

	static {
		var interval = NumInRange.builder()
			.min(1)
			.max(Duration.of(5, ChronoUnit.MINUTES).toSeconds())
			.build();
		var size = NumInRange.builder()
			.min(1)
			.max(1000)
			.build();
		VALID_BATCH = batchConfig -> interval.test(batchConfig.getInterval())
			&& size.test(batchConfig.getSize());
	}

	@Override
	public boolean test(RemoteConfig pluginConfig)
	{
		if (pluginConfig == null || pluginConfig.getLastUpdated() == null)
		{
			return false;
		}

		for (var entry : pluginConfig.getBatchConfigs().entrySet())
		{
			if (!VALID_BATCH.test(entry.getValue()))
			{
				return false;
			}
		}

		return true;
	}
}
