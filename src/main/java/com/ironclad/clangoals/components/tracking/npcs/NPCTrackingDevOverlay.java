package com.ironclad.clangoals.components.tracking.npcs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.status.StatusMessage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ChatMessageType;
import net.runelite.api.NPC;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class NPCTrackingDevOverlay extends Overlay implements Component
{
	private final NPCTrackingComponent plugin;
	private final ModelOutlineRenderer renderer;
	private final OverlayManager overlayManager;
	private final EventBus eventBus;
	private final boolean devMode;
	private boolean enabled;
	private final Map<Integer, NPC> npcs = new HashMap<>();

	@Inject
	NPCTrackingDevOverlay(NPCTrackingComponent plugin,
						  ModelOutlineRenderer renderer,
						  OverlayManager overlayManager,
						  EventBus eventBus,
						  @Named("developerMode") boolean devMode)
	{
		this.overlayManager = overlayManager;
		this.eventBus = eventBus;
		this.devMode = devMode;
		this.plugin = plugin;
		this.renderer = renderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return this.devMode; //Only enable this component when RuneLite is in developer mode
	}

	@Override
	public void onStartUp(PluginState state)
	{
		this.overlayManager.add(this);
		this.eventBus.register(this);
	}

	@Override
	public void onShutDown(PluginState state)
	{
		this.overlayManager.remove(this);
		this.eventBus.unregister(this);
		this.npcs.clear();
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted e)
	{
		if (e.getCommand().equalsIgnoreCase("icdebugnpcs"))
		{
			this.enabled = !this.enabled;
			this.eventBus.post(StatusMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.value(new ChatMessageBuilder()
					.append("NPC tracking overlay ")
					.append(this.enabled ? Color.GREEN : Color.RED, this.enabled ? "enabled" : "disabled")
					.build())
				.build());
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned npc)
	{
		this.npcs.put(npc.getNpc().getIndex(), npc.getNpc());
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned npc)
	{
		this.npcs.remove(npc.getNpc().getIndex());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!this.enabled)
		{
			return null;
		}

		int count = 1;

		for (Map.Entry<Integer, TrackedNpc> entry : this.plugin.getTrackedNpcs().entrySet())
		{
			int index = entry.getKey();
			TrackedNpc trackedNpc = entry.getValue();
			String message = String.format("%d/%d | IDX: %d ID: %d | M: %s", count++, this.plugin.getTrackedNpcs().size(),
				trackedNpc.getIndex(),
				trackedNpc.getId(),
				trackedNpc.isMissing()
			);
			this.npcs.computeIfPresent(index, (i, npc) ->
			{
				this.renderer.drawOutline(npc, 5, trackedNpc.creditMe() ? Color.GREEN : Color.RED, 3);
				OverlayUtil.renderActorOverlay(graphics, npc, message, Color.GREEN); //This also adds a tile overlay, but idc
				return npc;
			});
		}
		return null;
	}
}
