package com.ironclad.clangoals.components.service.config.predicate;

import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class NumInRange implements Predicate<Number>
{
	@NonNull
	private final Number min;
	@NonNull
	private final Number max;

	@Override
	public boolean test(Number number)
	{
		return number != null && min.doubleValue() >= number.doubleValue() && number.doubleValue() <= max.doubleValue();
	}
}
