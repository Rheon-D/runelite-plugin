package com.ironclad.clangoals.components.service;

import lombok.Value;

@Value
public class PluginStateChanged
{
	PluginState previous;
	PluginState current;
}
