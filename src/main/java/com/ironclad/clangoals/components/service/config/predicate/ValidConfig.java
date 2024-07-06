package com.ironclad.clangoals.components.service.config.predicate;

import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ValidConfig implements Predicate<RemoteConfig>
{
	private static final Predicate<BatchConfig> VALID_BATCH;

	static {
		var interval = NumInRange.builder()
			.min(1000)
			.max(Duration.of(10, ChronoUnit.MINUTES).toMillis())
			.build();
		var size = NumInRange.builder()
			.min(1)
			.max(1000)
			.build();
		VALID_BATCH = batchConfig -> interval.test(batchConfig.getInterval()) && size.test(batchConfig.getSize());
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
