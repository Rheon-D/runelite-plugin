package com.ironclad.clangoals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class PluginProperties
{
	private static final String VERSION_MAJOR = "version.major";
	private static final String VERSION_MINOR = "version.minor";
	private static final String VERSION_PATCH = "version.patch";
	private static final String VERSION_STRING = "version.string";
	private static final String API_BASE = "api.base";

	@Getter(AccessLevel.PACKAGE)
	private static final Properties properties = new Properties();

	static
	{
		try (InputStream in = PluginProperties.class.getResourceAsStream("plugin.properties"))
		{
			properties.load(in);
			properties.put(VERSION_STRING, String.format("%s.%s.%s",
				properties.get(VERSION_MAJOR),
				properties.get(VERSION_MINOR),
				properties.get(VERSION_PATCH))
			);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
