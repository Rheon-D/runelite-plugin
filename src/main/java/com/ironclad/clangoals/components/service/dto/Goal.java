package com.ironclad.clangoals.components.service.dto;

import lombok.Value;

//Unused
@Value//TODO: Get goals from server
public class Goal
{
	Type type;
	double progress;
	double total;
	String target;

	public enum Type{
		//TODO, Icon provider for each type.
		NPC,
		LOOT,
		XP;
	}
}
