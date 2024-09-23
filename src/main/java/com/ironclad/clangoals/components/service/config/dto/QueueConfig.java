package com.ironclad.clangoals.components.service.config.dto;

import com.ironclad.clangoals.components.service.config.Updatable;
import com.ironclad.clangoals.util.predicate.NumInRange;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueConfig implements Updatable<QueueConfig>
{
	/**
	 * Maximum number of items to batch together before sending.
	 */
	int size;
	/**
	 * Interval in seconds between sending batches.
	 */
	long interval;

	@Override
	public void update(QueueConfig other)
	{
		this.size = other.size;
		this.interval = other.interval;
	}

	public static final Predicate<QueueConfig> VALIDATOR;

	static {
		var interval = NumInRange.builder()
			.min(1)
			.max(Duration.of(5, ChronoUnit.MINUTES).toSeconds())
			.build();
		var size = NumInRange.builder()
			.min(1)
			.max(1000)
			.build();
		VALIDATOR = batchConfig -> interval.test(batchConfig.getInterval())
			&& size.test(batchConfig.getSize());
	}
}
