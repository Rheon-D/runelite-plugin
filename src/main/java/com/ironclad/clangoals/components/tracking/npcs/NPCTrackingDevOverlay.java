package com.ironclad.clangoals.components.tracking.npcs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
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
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
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
	//static boolean IS_A_FUCKING_MESS = definitely;
	private static final Color TRACKED_COLOR = new Color(0, 255, 0, 100);
	private static final Color INTERACTING_COLOR = new Color(255, 128, 0, 100);
	private final NPCTrackingComponent plugin;
	private final ModelOutlineRenderer renderer;
	private final OverlayManager overlayManager;
	private final ChatMessageManager chatMessageManager;
	private final EventBus eventBus;
	private final boolean devMode;
	private boolean enabled;
	private final Map<Integer, NPC> npcs = new HashMap<>();

	@Inject
	NPCTrackingDevOverlay(NPCTrackingComponent plugin,
						  ModelOutlineRenderer renderer,
						  OverlayManager overlayManager,
						  ChatMessageManager chatMessageManager, EventBus eventBus,
						  @Named("developerMode") boolean devMode)
	{
		this.overlayManager = overlayManager;
		this.chatMessageManager = chatMessageManager;
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
		return devMode; //Only enable this component when RuneLite is in developer mode
	}

	@Override
	public void onStartUp(PluginState state)
	{
		overlayManager.add(this);
		eventBus.register(this);
	}

	@Override
	public void onShutDown(PluginState state)
	{
		overlayManager.remove(this);
		eventBus.unregister(this);
		npcs.clear();
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted e)
	{
		if (e.getCommand().equalsIgnoreCase("debugnpcs"))
		{
			enabled = !enabled;
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.value("Npc debug overlay: " + (enabled ? "enabled" : "disabled"))
				.build());
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned npc)
	{
		npcs.put(npc.getNpc().getIndex(), npc.getNpc());
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned npc)
	{
		npcs.remove(npc.getNpc().getIndex());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!enabled)
		{
			return null;
		}

		int count = 1;

		for (Map.Entry<Integer, TrackedNpc> entry : plugin.getTrackedNpcs().entrySet())
		{
			int index = entry.getKey();
			TrackedNpc trackedNpc = entry.getValue();
			Color color = trackedNpc.isMyKill() ? TRACKED_COLOR : INTERACTING_COLOR;
			String message = String.format("%d/%d | IDX: %d ID: %d DM: %d DO: %d | M: %s", count++, plugin.getTrackedNpcs().size(),
				trackedNpc.getIndex(),
				trackedNpc.getId(),
				trackedNpc.getDamageMe(),
				trackedNpc.getDamageOther(),
				trackedNpc.isMissing()
			);
			npcs.computeIfPresent(index, (i, npc) ->
			{
				renderer.drawOutline(npc, 5, color, 3);
				OverlayUtil.renderActorOverlay(graphics, npc, message, color); //This also adds a tile overlay, but idc
				return npc;
			});
		}
		return null;
	}
}
