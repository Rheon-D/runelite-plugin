package com.ironclad.clangoals.components.tracking;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.api.Endpoint;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.util.BatchQueue;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class NpcTrackingComponent extends AbstractTrackingComponent<NPC>
{
	private final Set<Actor> trackedNpcs;
	private final Client client;

	@Inject
	public NpcTrackingComponent(ApiService api, EventBus eventBus, Client client)
	{
		super(BatchConfig.Type.NPC, api, eventBus);
		trackedNpcs = Sets.newHashSet();
		this.client = client;
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		trackedNpcs.clear();
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return state.isAuthenticated() && state.isInGame() && state.isInEnabledWorld();
	}

	protected void onFlush(List<NPC> pluginNPCS)
	{
		this.api.batchUpdate(Endpoint.KILLS, pluginNPCS, (item) -> {
			JsonObject tmp = new JsonObject();
			tmp.addProperty("npc_id", item.getId());
			tmp.addProperty("name", item.getName());
			return tmp;
		});
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				getQueue().flush();
				break;
		}
	}

	@Subscribe
	private void onInteractingChanged(InteractingChanged e)
	{
		//TODO: Test me
		Actor source = e.getSource();
		Actor target = e.getTarget();
		boolean anyLP = source == client.getLocalPlayer() || target == client.getLocalPlayer();
		boolean anyNpc = source instanceof NPC || target instanceof NPC;

		if (!anyLP || !anyNpc)
		{
			return;
		}
		trackedNpcs.add(e.getTarget());
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();
		if (npc.isDead())
		{
			//We know we hit this thing at some point, and it just died...
			logKill(npc);
		}
		//Yeet it if it despawns, we can't track it anymore
		//Could change this removal to a time-out
		trackedNpcs.remove(npc);
	}

	@Subscribe
	private void onActorDeath(ActorDeath e)
	{
		if (!(e.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) e.getActor();

		if (trackedNpcs.contains(npc))
		{
			logKill(npc);
		}
	}

	private void logKill(NPC npc)
	{
		getQueue().addItem(npc);
		log.debug("Logged kill for {}", npc);
	}


}
