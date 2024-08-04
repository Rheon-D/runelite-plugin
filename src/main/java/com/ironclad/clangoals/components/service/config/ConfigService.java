package com.ironclad.clangoals.components.service.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.util.predicate.ValidConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@Singleton
public final class ConfigService
{
	public static final RemoteConfig DEFAULT_CONFIG;
	private static final ValidConfig VALID_CONFIG = new ValidConfig();
	private static final int INTERVAL_SEC = 60;

	static
	{
		DEFAULT_CONFIG = new RemoteConfig(Instant.EPOCH, false, new EnumMap<>(BatchConfig.Type.class));
		DEFAULT_CONFIG.getBatchConfigs().put(BatchConfig.Type.XP, BatchConfig.builder()
			.size(100)
			.interval(Duration.of(150, ChronoUnit.SECONDS).toMillis())
			.enabled(true)
			.build());
		DEFAULT_CONFIG.getBatchConfigs().put(BatchConfig.Type.NPC, BatchConfig.builder()
			.size(10)
			.interval(Duration.of(15, ChronoUnit.SECONDS).toMillis())
			.enabled(true)
			.build());
		DEFAULT_CONFIG.getBatchConfigs().put(BatchConfig.Type.ITEM, BatchConfig.builder()
			.size(10)
			.interval(Duration.of(15, ChronoUnit.SECONDS).toMillis())
			.enabled(true)
			.build());
	}

	private final ApiService api;
	private final EventBus eventBus;
	private final ScheduledExecutorService executor;

	private volatile RemoteConfig configuration = DEFAULT_CONFIG;
	private final CompletableFuture<RemoteConfig> firstRunFuture = new CompletableFuture<>();

	@Inject
	public ConfigService(ApiService api, EventBus eventBus, ScheduledExecutorService executor)
	{
		this.api = api;
		this.eventBus = eventBus;
		this.executor = executor;
		executor.scheduleAtFixedRate(this::tick, 0, INTERVAL_SEC, TimeUnit.of(ChronoUnit.SECONDS));
	}

	public void shutdown()
	{
		if (executor != null)
		{
			executor.shutdown();
		}
	}

	private void tick()
	{
		try
		{
			if (api.isAuthenticated())
			{
				log.debug("Checking for updated configuration");
				RemoteConfig config = api.getPluginConfiguration();
				if (config != null && VALID_CONFIG.test(config))
				{
					setConfiguration(config);
				}
				else
				{
					log.debug("Invalid configuration from server, using default.");
				}
			}
		}
		finally
		{
			firstRunFuture.complete(configuration);
		}
	}

	private void setConfiguration(@NonNull RemoteConfig config)
	{
		if (config.getLastUpdated().isAfter(configuration.getLastUpdated()))
		{
			RemoteConfig old = configuration;
			configuration = config;
			log.debug("Updated configuration: {}", configuration);
			eventBus.post(new RemoteConfigChanged(old, configuration));
		}
	}

	public RemoteConfig getConfiguration()
	{
		if (!firstRunFuture.isDone())
		{
			try
			{
				return firstRunFuture.get(5, TimeUnit.SECONDS);
			}
			catch (Exception ex)
			{
				log.warn("Failed to get configuration from server, using default configuration");
				return DEFAULT_CONFIG;
			}
		}
		return configuration;
	}


}
