package com.ironclad.clangoals.components.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.RemoteConfigLoader;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.util.ClanUtils;
import com.ironclad.clangoals.util.WorldUtils;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import java.util.concurrent.ScheduledExecutorService;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.AccountHashChanged;
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
	private static final PluginState DEFAULT_STATE = PluginState.builder().maintenance(true).build();
	private static final ValidApiKey VALID_API_KEY = new ValidApiKey();

	private final ApiService api;
	private final RemoteConfigLoader rConfigLoader;
	private final IroncladClanGoalsConfig pluginConfig;
	private final Client client;
	private final EventBus eventBus;
	private final ClientThread clientThread;
	private final ScheduledExecutorService executor;

	@Getter
	private PluginState state = DEFAULT_STATE;

	@Override
	public void onStartUp(PluginState state)
	{
		this.eventBus.register(this);
		onRemoteConfigChanged(new RemoteConfigChanged(this.rConfigLoader.getManagedConfig()));
		verifyApiKey(this.pluginConfig.apiKey(), false);
	}

	@Override
	public void onShutDown(PluginState state)
	{
		this.api.checkAuth(null);
		this.eventBus.unregister(this);
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
		this.eventBus.post(new PluginStateChanged(old, this.state));
		log.debug("PluginStateChanged: {}", this.state);
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
				verifyApiKey(this.pluginConfig.apiKey(), false);
			case HOPPING:
				setState(this.state.toBuilder()
						.inGame(false)
						.inClan(false)
						.inEnabledWorld(false)
						.build(),
					false);
				break;
			case LOGGED_IN:
				onLoggedIn();
				break;
		}
	}

	@Subscribe
	private void onAccountHashChanged(AccountHashChanged e)
	{
		this.api.setAccountHash(this.client.getAccountHash());
	}

	private void onLoggedIn()
	{
		this.clientThread.invokeLater(() -> {
			//Available immediately, display names are not
			//Patience we must have.
			Player player = this.client.getLocalPlayer();
			if (player == null || Strings.isNullOrEmpty(player.getName()))
			{
				return false;
			}

			setState(this.state.toBuilder()
					.inGame(true)
					.inEnabledWorld(!WorldUtils.isDisabledWorldType(this.rConfigLoader.getManagedConfig(), this.client.getWorldType()))
					.build(),
				false);

			long currHash = this.api.getAccountHash();
			long newHash = this.client.getAccountHash();

			if (currHash != newHash)
			{
				this.api.setAccountHash(newHash);
				this.api.updatePlayerAsync(player.getName());
			}

			return true;
		});
	}

	@Subscribe
	private void onClanChannelChanged(ClanChannelChanged e)
	{
		setState(this.state.toBuilder().inClan(ClanUtils.isMemberOfClan(this.client)).build(), false);
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

			if (newValue == null || !newValue.equals(event.getOldValue()))
			{
				verifyApiKey(newValue, true);
			}
		}
	}

	@Subscribe(priority = Float.MAX_VALUE)
	private void onRemoteConfigChanged(RemoteConfigChanged e)
	{
		setState(this.state.toBuilder()
			.maintenance(e.getConfig().isMaintenance())
			.build(), false);
	}

	private void verifyApiKey(String key, boolean force)
	{
		if (this.api.isAuthenticated() && !force)
		{
			return;
		}
		this.executor.execute(() -> {
			boolean result = this.api.checkAuth(key);
			setState(this.state.toBuilder().authenticated(result).build(), false);
		});
	}
}
