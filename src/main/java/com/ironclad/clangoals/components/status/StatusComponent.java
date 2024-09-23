package com.ironclad.clangoals.components.status;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.PluginStateChanged;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.util.ClanUtils;
import com.ironclad.clangoals.util.WorldUtils;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class StatusComponent implements Component
{
	private final EventBus eventBus;
	private final Client client;
	private final ChatMessageManager chatMessageManager;
	private final String pluginName;
	private final RemoteConfig remoteConfig;
	private final IroncladClanGoalsConfig pluginConfig;
	private boolean wasMaintenance = false;
	private boolean statusReady = false;
	private boolean isAuthenticated = false;

	private String lastMessage = "";

	@Inject
	public StatusComponent(EventBus eventBus,
						   Client client,
						   ChatMessageManager chatMessageManager,
						   @Named("plugin.name") String pluginName,
						   RemoteConfig remoteConfig,
						   IroncladClanGoalsConfig pluginConfig)
	{
		this.eventBus = eventBus;
		this.client = client;
		this.chatMessageManager = chatMessageManager;
		this.pluginName = pluginName;
		this.remoteConfig = remoteConfig;
		this.pluginConfig = pluginConfig;
	}

	@Override
	public void onStartUp(PluginState state)
	{
		this.eventBus.register(this);
		updateStatus(); //Should only fire ingame anyway.
	}

	@Override
	public void onShutDown(PluginState state)
	{
		this.eventBus.unregister(this);
	}

	private void updateStatus()
	{
		if (!this.statusReady)
		{
			return;
		}

		boolean disabledWorld = WorldUtils.isDisabledWorldType(this.client.getWorldType());
		boolean maintenance = this.remoteConfig.isMaintenance();
		boolean noAuth = !this.isAuthenticated;
		boolean noConfig = Strings.isNullOrEmpty(this.pluginConfig.apiKey());
		boolean error = disabledWorld || noAuth || maintenance || noConfig;

		ChatMessageBuilder builder = new ChatMessageBuilder();
		builder
			.append(ChatColorType.HIGHLIGHT)
			.append("Clan Goals");

		if (error)
		{
			builder.append(" are disabled ");
			if(noConfig){
				builder.append("invalid API Key.");
			}
			else if (maintenance)
			{
				builder.append("due to maintenance.");
			}
			else if (noAuth)
			{
				builder.append("as you aren't authenticated: Please check your API key in the plugin configuration.");
			}
			if (disabledWorld)
			{
				builder.append("on this world.");
			}
		}
		else
		{
			builder.append(" Enabled!");
		}

		String message = builder.build();
		if(!message.equals(this.lastMessage))
		{
			queueMessage(message);
			this.lastMessage = message;
		}
	}

	private void queueMessage(QueuedMessage message)
	{
		if (this.statusReady)
		{
			this.chatMessageManager.queue(message);
		}
	}

	private void queueMessage(String message)
	{
		queueMessage(QueuedMessage.builder()
			.type(ChatMessageType.CLAN_MESSAGE)
			.name(this.pluginName)
			.sender(this.pluginName)
			.value(message)
			.runeLiteFormattedMessage(message)
			.build());
	}

	@Subscribe
	private void onStatusMessage(StatusMessage e)
	{
		queueMessage(QueuedMessage.builder()
			.type(e.getType())
			.name(this.pluginName)
			.sender(MoreObjects.firstNonNull(e.getSender(), this.pluginName))
			.value(e.getValue())
			.build());
	}

	@Subscribe
	private void onClanChannelChanged(ClanChannelChanged e)
	{
		if (ClanUtils.isMemberOfClan(this.client))
		{
			this.statusReady = true;
			updateStatus();
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				this.statusReady = false;
				break;
		}
	}

	@Subscribe
	private void onPluginStateChanged(PluginStateChanged e)
	{
		this.statusReady = e.getCurrent().isInClan();

		this.isAuthenticated = e.getCurrent().isAuthenticated();

		if (!this.statusReady)
		{
			return;
		}

		PluginState prev = e.getPrevious();
		PluginState curr = e.getCurrent();

		if (curr.isAuthenticated() != prev.isAuthenticated())
		{
			this.updateStatus();
		}
	}

	@Subscribe
	private void onRemoteConfigChanged(RemoteConfigChanged e)
	{
		if (!this.statusReady)
		{
			return;
		}

		boolean isMaintenance = e.getConfig().isMaintenance();
		boolean changed = isMaintenance != this.wasMaintenance;
		if (changed)
		{
			this.updateStatus();
			this.wasMaintenance = isMaintenance;
		}
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return config.statusMessages();
	}
}
