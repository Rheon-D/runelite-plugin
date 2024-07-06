package com.ironclad.clangoals.components.service.api;

public enum Endpoint
{
	ME("me"),
	CHARACTERS("characters"),

	CONFIGURATION("configuration"),
	BATCH("batch"),
	XP("xp"),
	LOOT("loot"),
	KILLS("kills"),
	GOALS("goals");

	private final String path;

	Endpoint(String path)
	{
		this.path = path;
	}

	public String getPath()
	{
		return path;
	}
}

