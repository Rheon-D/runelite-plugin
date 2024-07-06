package com.ironclad.clangoals.components.service.config;

import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import lombok.Value;

@Value
public class RemoteConfigChanged
{
	RemoteConfig previous;
	RemoteConfig current;
}
