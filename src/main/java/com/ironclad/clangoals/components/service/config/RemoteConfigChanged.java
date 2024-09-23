package com.ironclad.clangoals.components.service.config;

import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import lombok.Value;

@Value
public class RemoteConfigChanged
{
	RemoteConfig config;
}
