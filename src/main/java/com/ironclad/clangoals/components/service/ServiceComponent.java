package com.ironclad.clangoals.components.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.IroncladClanGoalsPlugin;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.util.ClanUtils;
import com.ironclad.clangoals.util.WorldUtils;
import java.util.concurrent.CompletableFuture;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServiceComponent implements Component
{
	private static final PluginState DEFAULT_STATE = PluginState.builder().remoteConfig(ConfigService.DEFAULT_CONFIG).build();

	private final ApiService api;
	private final ConfigService configService;
	private final IroncladClanGoalsPlugin plugin;
	private final IroncladClanGoalsConfig pluginConfig;
	private final Client client;
	private final EventBus eventBus;
	@Getter
	private PluginState state = DEFAULT_STATE;

	@Override
	public void onStartUp(PluginState state)
	{
		eventBus.register(this);
		authenticate();
	}

	@Override
	public void onShutDown(PluginState state)
	{
		eventBus.unregister(this);
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
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
				api.updatePlayer(null);
				setState(state.toBuilder()
						.inGame(false)
						.inClan(false)
						.inEnabledWorld(false)
						.build(),
					false);
				break;
			case LOGGING_IN:
			case LOADING:
			case CONNECTION_LOST:
			case HOPPING:
				setState(state.toBuilder()
						.inGame(false)
						.inClan(false)
						.inEnabledWorld(false)
						.build(),
					false);
				break;
			case LOGGED_IN:
				Player player = client.getLocalPlayer();
				if (player == null)
				{
					break; //Shouldn't happen.
				}

				setState(state.toBuilder()
						.inGame(true)
						.inClan(ClanUtils.isMemberOfClan(client))
						.inEnabledWorld(!WorldUtils.isDisabledWorldType(client.getWorldType()))
						.build(),
					false);

				long currHash = api.getAccountHash();
				long newHash = client.getAccountHash();

				if (currHash == newHash)
				{
					break;
				}

				api.updatePlayer(player);

				break;
		}
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

			if (!Strings.isNullOrEmpty(newValue) && !newValue.equals(event.getOldValue()))
			{
				authenticate();
			}
		}
	}

	private void authenticate()
	{
		CompletableFuture<Boolean> authenticated = api.authenticate(pluginConfig.apiKey());
		authenticated.thenAccept((res) -> {
			//This occurs off client thread and on enable, so use the old game state values.
			setState(state.toBuilder().authenticated(res).build(), true);
			if(res) {
				configService.loadConfiguration().thenAccept((config) -> {
					RemoteConfig oldConfig = state.getRemoteConfig();
					if(oldConfig.equals(config) || oldConfig.getLastUpdated().isAfter(config.getLastUpdated())){
						return; //Same, same or older.
					}
					setState(state.toBuilder().remoteConfig(config).build(), true);
					eventBus.post(new RemoteConfigChanged(oldConfig, config));
				});
			}
		});
	}
}
