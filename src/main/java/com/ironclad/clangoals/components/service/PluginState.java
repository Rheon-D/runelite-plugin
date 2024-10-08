package com.ironclad.clangoals.components.service;

import com.ironclad.clangoals.components.service.api.ApiService;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public class PluginState
{
	/**
	 * Has the {@link ApiService} been provided with
	 * a valid API key.
	 */
	boolean authenticated;

	boolean maintenance;

	boolean inGame;
	/**
	 * Is the player in the IronClad clan.
	 */
	boolean inClan;
	/**
	 * Is the player in a world where goals are enabled.
	 */
	boolean inEnabledWorld;//TODO: Make this a per goal setting.
}
