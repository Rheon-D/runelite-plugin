package com.ironclad.clangoals.components.service;

import lombok.NonNull;
import lombok.Value;

@Value
public class PluginStateChanged
{

	@NonNull
	PluginState previous;

	@NonNull
	PluginState current;
}
