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
import com.ironclad.clangoals.util.predicate.ConfigValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import javax.inject.Named;
import joptsimple.internal.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@Singleton
public final class RemoteConfigLoader
{
	public static final RemoteConfig EMPTY_CONFIG = new RemoteConfig(Instant.EPOCH, 10, true, XpTrackingConfig.getEmpty(), ItemTrackingConfig.getEmpty(), NPCTrackingConfig.getEmpty());
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

	CompletableFuture<RemoteConfig> fetchConfiguration()
	{
		log.info("Checking for updated configuration");
		CompletableFuture<RemoteConfig> future = new CompletableFuture<>();

		if (this.developerMode && Environment.REMOTE_CONF.isSet())
		{
			String file = Environment.REMOTE_CONF.get();
			if (!Strings.isNullOrEmpty(file))
			{
				try
				{
					String strConf = Files.readString(Paths.get(file));
					RemoteConfig conf = this.gson.fromJson(strConf, RemoteConfig.class);
					log.info("Using local remote config: {}", conf);
					updateConfiguration(conf);
					future.complete(conf);
					return future;
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
			future.complete(this.managedConfig);
			return future;
		}

		this.api.getPluginConfiguration(this.pluginConfig.apiKey()).thenAccept(config -> {
			if (config != null && CONFIG_VALIDATOR.test(config))
			{
				updateConfiguration(config);
				future.complete(config);
			}
			else
			{
				revert("Invalid configuration");
				future.complete(this.managedConfig);
			}
		}).exceptionally(ex -> {
			log.error("Failed to fetch configuration", ex);
			future.complete(this.managedConfig);
			return null;
		});

		return future;
	}

	private void revert(String reason)
	{
		log.info("Reverting to maintenance mode: {}", reason);
		updateConfiguration(EMPTY_CONFIG);
	}

	private void updateConfiguration(@NonNull RemoteConfig config)
	{
		if (config.getLastUpdated().isAfter(this.managedConfig.getLastUpdated()))
		{
			this.managedConfig.update(config);
			log.info("Updated configuration: {}", this.managedConfig);
			this.eventBus.post(new RemoteConfigChanged(this.managedConfig));
		}
	}

	public RemoteConfig getManagedConfig()
	{
		return this.managedConfig;
	}
}
