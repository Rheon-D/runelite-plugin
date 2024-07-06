package com.ironclad.clangoals.components.service.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.Value;

@Value
public class RemoteConfig
{
	Instant lastUpdated;
	boolean maintenance;
	Map<BatchConfig.Type, BatchConfig> batchConfigs;
}
