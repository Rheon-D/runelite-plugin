package com.ironclad.clangoals.predicate;

import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.util.predicate.NumInRange;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import com.ironclad.clangoals.util.predicate.ValidConfig;
import java.time.Instant;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PredicateTest
{
	@Test
	public void testValidApiKey()
	{
		ValidApiKey validApiKey = new ValidApiKey();
		assertTrue(validApiKey.test("1234567890abcdef1234"));
		assertFalse(validApiKey.test("1234567890abcdef123"));
		assertFalse(validApiKey.test("123456=890abcdef1234"));
		assertFalse(validApiKey.test(" "));
		assertFalse(validApiKey.test(null));
	}

	@Test
	public void testNumInRange()
	{
		 NumInRange numInRange = NumInRange.builder().min(1).max(10).build();
		 assertTrue(numInRange.test(5));
		 assertFalse(numInRange.test(11));
		 assertFalse(numInRange.test(0));
		 assertFalse(numInRange.test(null));
	}

	@Test
	public void testValidConfig()
	{
		ValidConfig validConfig = new ValidConfig();
		assertTrue(validConfig.test(ConfigService.DEFAULT_CONFIG));
		assertFalse(validConfig.test(null));
		assertFalse(validConfig.test(new RemoteConfig(null, true, null)));
	}
}
