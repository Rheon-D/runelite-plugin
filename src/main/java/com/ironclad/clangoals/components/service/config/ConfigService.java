package com.ironclad.clangoals.components.service.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.components.service.config.predicate.ValidConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class ConfigService
{
	public static final RemoteConfig DEFAULT_CONFIG;
	private static final ValidConfig VALID_CONFIG = new ValidConfig();

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
	@Getter
	private RemoteConfig configuration = DEFAULT_CONFIG;

	public CompletableFuture<RemoteConfig> loadConfiguration()
	{
		var future = new CompletableFuture<RemoteConfig>();
		var response = api.getPluginConfiguration();
		response.thenAccept((config) -> {
			// *cough* CrowdStrike *cough*
			configuration = VALID_CONFIG.test(config) ? config : configuration;
			future.complete(configuration);
		}).exceptionally((ex) -> {
			log.warn("Failed to load configuration from server, using default configuration", ex);
			future.complete(configuration);
			return null;
		});
		return future;
	}
}
