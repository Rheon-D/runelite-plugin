package com.ironclad.clangoals.util.predicate;

import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingConfig;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingConfig;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingConfig;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

//TODO I probably should change all this nonsense to a custom Type Adapter, but im lazy
@RequiredArgsConstructor
public class ConfigValidator implements Predicate<RemoteConfig>
{
	private static final NumInRange REFRESH_VALID = new NumInRange(1,60);

	@Override
	public boolean test(RemoteConfig pluginConfig)
	{
		if (pluginConfig == null || pluginConfig.getLastUpdated() == null)
		{
			return false;
		}

		if(!REFRESH_VALID.test(pluginConfig.getRefreshInterval()))
		{
			return false;
		}

		if(!ItemTrackingConfig.VALIDATOR.test(pluginConfig.getItemTrackingConfig()))
		{
			return false;
		}

		if(!XpTrackingConfig.VALIDATOR.test(pluginConfig.getXpTrackingConfig()))
		{
			return false;
		}

		return NPCTrackingConfig.VALIDATOR.test(pluginConfig.getNpcTrackingConfig());
	}
}
