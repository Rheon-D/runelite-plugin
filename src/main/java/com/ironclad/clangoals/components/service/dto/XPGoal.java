package com.ironclad.clangoals.components.service.dto;

import java.util.UUID;
import lombok.Value;

@Value
public class XPGoal
{
	UUID uuid;
	String skill;
	int progress;
	int goal;
}
