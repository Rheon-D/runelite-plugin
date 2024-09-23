package com.ironclad.clangoals.util;

import joptsimple.internal.Strings;

public enum Environment
{
	REMOTE_CONF("ironclad.rconf"),
	DEV_URL("ironclad.api.base"),
	DEV_KEY("ironclad.api.key");

	final String key;

	Environment(String key)
	{
		this.key = key;
	}

	public final String get()
	{
		return System.getenv(this.key);
	}

	public boolean isSet(){
		return !Strings.isNullOrEmpty(get());
	}
}
