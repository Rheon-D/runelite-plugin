package com.ironclad.clangoals.components.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.util.ClanUtils;
import com.ironclad.clangoals.util.WorldUtils;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServiceComponent implements Component
{
	private static final PluginState DEFAULT_STATE = PluginState.builder().remoteConfig(ConfigService.DEFAULT_CONFIG).build();
	private static final ValidApiKey VALID_API_KEY = new ValidApiKey();

	private final ApiService api;
	private final ConfigService configService;
	private final IroncladClanGoalsConfig pluginConfig;
	private final Client client;
	private final EventBus eventBus;
	private final ClientThread clientThread;

	@Getter
	private PluginState state = DEFAULT_STATE;

	@Override
	public void onStartUp(PluginState state)
	{
		eventBus.register(this);
		tryAuthenticate(pluginConfig.apiKey());
	}

	@Override
	public void onShutDown(PluginState state)
	{
		eventBus.unregister(this);
		configService.shutdown();
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return true;
	}

	private void setState(PluginState state, boolean force)
	{
		PluginState old = this.state;
		if (old.equals(state) && !force)
		{
			return;
		}
		this.state = state;
		eventBus.post(new PluginStateChanged(old, this.state));
		log.debug("PluginStateChanged: {}", this.state);
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
				api.refreshAccountHash();
			case LOADING:
			case HOPPING:
				setState(state.toBuilder()
						.inGame(false)
						.inClan(false)
						.inEnabledWorld(false)
						.build(),
					true);
				break;
			case LOGGED_IN:
				clientThread.invokeLater(() -> {
					//Available immediately, display names are not
					//Patience we must have.
					Player player = client.getLocalPlayer();
					if(player == null || Strings.isNullOrEmpty(player.getName()))
					{
						return false;
					}

					setState(state.toBuilder()
							.inGame(true)
							.inEnabledWorld(!WorldUtils.isDisabledWorldType(client.getWorldType()))
							.build(),
						false);

					long currHash = api.getAccountHash();
					long newHash = client.getAccountHash();

					if (currHash != newHash)
					{
						api.refreshAccountHash();
						api.updatePlayer(player.getName());
					}

					return true;
				});
				break;
		}
	}

	@Subscribe
	private void onClanChannelChanged(ClanChannelChanged event)
	{
		setState(state.toBuilder().inClan(ClanUtils.isMemberOfClan(client)).build(), false);
	}


	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!IroncladClanGoalsConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (IroncladClanGoalsConfig.API_KEY.equals(event.getKey()))
		{
			String newValue = event.getNewValue();

			if (VALID_API_KEY.test(newValue) && !newValue.equals(event.getOldValue()))
			{
				tryAuthenticate(newValue);
			}
		}
	}

	@Subscribe(priority = Float.MAX_VALUE)
	private void onRemoteConfigChanged(RemoteConfigChanged e){
		setState(state.toBuilder().remoteConfig(e.getCurrent()).build(), false);
	}

	private void tryAuthenticate(@NonNull String key)
	{
		log.debug("Trying authentication with key: {}", key);
		this.api.authenticateAsync(key)
			.thenAccept((authenticated) -> {
				setState(state.toBuilder().authenticated(authenticated).build(), false);
				if(authenticated){
					RemoteConfig config = configService.getConfiguration();
					setState(state.toBuilder().remoteConfig(config).build(), false);
				}
			});
	}
}
