package com.ironclad.clangoals.components.service;

import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public final class PluginState
{
	/**
	 * Has the @Link{ApiService} been provided with
	 * a valid API key.
	 */
	boolean authenticated;
	/**
	 * Remote config for the plugin.
	 * Default assigned by @Link{ConfigService}
	 */
	RemoteConfig remoteConfig;
	boolean inGame;
	/**
	 * Is the player in the IronClad clan.
	 */
	boolean inClan;
	/**
	 * Is the player in a world where goals are enabled.
	 * TODO: Make this a per goal setting.
	 */
	boolean inEnabledWorld;
}
