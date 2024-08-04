package com.ironclad.clangoals.components.service;

import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
@ToString
public final class PluginState
{
	/**
	 * Has the {@link ApiService} been provided with
	 * a valid API key.
	 */
	boolean authenticated;
	/**
	 * Remote config for the plugin.
	 * Default assigned by {@link ConfigService}
	 */
	@NonNull
	RemoteConfig remoteConfig;
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
