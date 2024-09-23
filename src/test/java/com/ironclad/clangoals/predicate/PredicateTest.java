package com.ironclad.clangoals.predicate;

import com.ironclad.clangoals.components.service.config.RemoteConfigLoader;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingConfig;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingConfig;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingConfig;
import com.ironclad.clangoals.util.predicate.NumInRange;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import com.ironclad.clangoals.util.predicate.ConfigValidator;
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
		ConfigValidator validConfig = new ConfigValidator();
		RemoteConfig conf = RemoteConfig.builder()
			.lastUpdated(Instant.EPOCH)
			.refreshInterval(10)
			.maintenance(true)
			.xpTrackingConfig(XpTrackingConfig.DEFAULT)
			.itemTrackingConfig(ItemTrackingConfig.DEFAULT)
			.npcTrackingConfig(NPCTrackingConfig.DEFAULT)
			.build();
		assertTrue(validConfig.test(conf));
		assertFalse(validConfig.test(null));
		assertFalse(validConfig.test(new RemoteConfig(null, 1, true, null, null, null)));
		conf.setRefreshInterval(600);
		assertFalse(validConfig.test(conf));
	}
}
