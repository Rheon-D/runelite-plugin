package com.ironclad.clangoals.components.service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrackingConfig
{
	int size;
	long interval;
	boolean enabled;

	public enum Type
	{
		ITEM,
		XP,
		NPC
	}
}
