package com.ironclad.clangoals.util;

import joptsimple.internal.Strings;

public enum Environment
{
	DEV_URL("ironclad.api.base"),
	DEV_KEY("ironclad.api.key");

	String key;

	Environment(String key)
	{
		this.key = key;
	}

	public final String get()
	{
		return System.getenv(key);
	}

	public boolean isSet(){
		return !Strings.isNullOrEmpty(get());
	}
}
