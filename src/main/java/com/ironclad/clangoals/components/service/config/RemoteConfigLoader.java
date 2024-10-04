package com.ironclad.clangoals.components.service.config;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingConfig;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingConfig;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingConfig;
import com.ironclad.clangoals.util.Environment;
import com.ironclad.clangoals.util.WorldUtils;
import com.ironclad.clangoals.util.predicate.ConfigValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import javax.inject.Named;
import joptsimple.internal.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@Singleton
public final class RemoteConfigLoader
{
	public static final RemoteConfig EMPTY_CONFIG = new RemoteConfig(Instant.EPOCH, 10, true, WorldUtils.DISABLED_WORLDS ,XpTrackingConfig.getEmpty(), ItemTrackingConfig.getEmpty(), NPCTrackingConfig.getEmpty());
	private static final ConfigValidator CONFIG_VALIDATOR = new ConfigValidator();

	private final ApiService api;
	private final EventBus eventBus;
	private final Gson gson;
	private final IroncladClanGoalsConfig pluginConfig;
	private final RemoteConfig managedConfig;
	private final boolean developerMode;

	@Inject
	public RemoteConfigLoader(ApiService api,
							  EventBus eventBus,
							  Gson gson,
							  IroncladClanGoalsConfig pluginConfig,
							  @Named("developerMode") boolean developerMode
	)
	{
		this.api = api;
		this.eventBus = eventBus;
		this.gson = gson;
		this.pluginConfig = pluginConfig;
		this.developerMode = developerMode;
		this.managedConfig = RemoteConfig.builder()
			.lastUpdated(Instant.EPOCH)
			.refreshInterval(10)
			.maintenance(true)
			.xpTrackingConfig(XpTrackingConfig.DEFAULT)
			.itemTrackingConfig(ItemTrackingConfig.DEFAULT)
			.npcTrackingConfig(NPCTrackingConfig.DEFAULT)
			.build();
		this.fetchConfiguration();
	}

	void fetchConfiguration()
	{
		log.debug("Checking for updated configuration");

		if (this.developerMode && Environment.REMOTE_CONF.isSet())
		{
			String file = Environment.REMOTE_CONF.get();
			if (!Strings.isNullOrEmpty(file))
			{
				try
				{
					String strConf = Files.readString(Paths.get(file));
					RemoteConfig conf = this.gson.fromJson(strConf, RemoteConfig.class);
					log.debug("Using local remote config: {}", conf);
					updateConfiguration(conf);
					return;
				}
				catch (IOException e)
				{
					throw new RuntimeException("failed to load override remote config", e);
				}
			}
		}

		if (Strings.isNullOrEmpty(this.pluginConfig.apiKey()))
		{
			revert("API key not set");
			return;
		}

		this.api.getPluginConfiguration(this.pluginConfig.apiKey()).thenAccept(config -> {
			if (config != null && CONFIG_VALIDATOR.test(config))
			{
				updateConfiguration(config);
			}
			else
			{
				revert("Invalid configuration");
			}
		}).exceptionally(ex -> {
			log.error("Failed to fetch configuration {}", ex.getMessage());
			return null;
		});
	}

	private void revert(String reason)
	{
		log.debug("Reverting to maintenance mode: {}", reason);
		updateConfiguration(EMPTY_CONFIG);
	}

	private void updateConfiguration(@NonNull RemoteConfig config)
	{
		if (config.getLastUpdated().isAfter(this.managedConfig.getLastUpdated()))
		{
			this.managedConfig.update(config);
			log.debug("Updated configuration: {}", this.managedConfig);
			this.eventBus.post(new RemoteConfigChanged(this.managedConfig));
		}
	}

	public RemoteConfig getManagedConfig()
	{
		return this.managedConfig;
	}
}
