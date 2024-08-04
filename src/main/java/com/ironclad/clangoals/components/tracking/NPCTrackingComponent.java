package com.ironclad.clangoals.components.tracking;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class NPCTrackingComponent extends AbstractTrackingComponent<TrackedNpc>
{
	private static final int YEET_TIME = 30;//Seconds
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, TrackedNpc> trackedNpcs;
	private final Client client;
	private final String endpoint;

	@Inject
	public NPCTrackingComponent(ApiService api,
								EventBus eventBus,
								Client client,
								@Named("api.endpoint.batch.npc") String endpoint)
	{
		super(BatchConfig.Type.NPC, api, eventBus);
		this.client = client;
		this.endpoint = endpoint;
		trackedNpcs = Maps.newHashMap();
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		trackedNpcs.clear();
	}

	protected void onFlush(List<TrackedNpc> pluginNPCS)
	{
		log.debug("Flushing Npc Queue");
		this.api.batchUpdate(endpoint, pluginNPCS, (item) -> {
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
				trackedNpcs.clear();
				break;
		}
	}

		/*
		AFAIK a player may only interact with one NPC at a time, but many npcs may interact with a player.
		Relying on interaction changed may result in kills be attributed to our player without us actually
		doing anything except stand there.

		Will try to track kills where our player has done damage to the NPC

		Complete accuracy probably isn't possible, but for mass amounts of npc kills it should be fine.
		Possible to track boss kc (likely a lower kill target) with more accuracy by tracking the kc message.
		 */
	//This would include stuff like bankers
		/*if (source == client.getLocalPlayer() && target instanceof NPC)
		{
			NPC npc = (NPC) target;
			NPCComposition composition = npc.getComposition();
			if(composition == null) return; //Reasons?
			trackedNpcs.computeIfAbsent(((NPC) target).getIndex(), index -> new TrackedNpc(npc.getId(), npc.getIndex(),composition.getName()));
			log.debug("We interacted changed: {} -> {}", source, target);
		}*/


	@Subscribe
	private void onHitsplatApplied(HitsplatApplied e)
	{
		if (blockTracking() || !(e.getActor() instanceof NPC))
		{
			return;
		}
		//Track all seen hitsplats.
		NPC npc = (NPC) e.getActor();

		trackedNpcs.computeIfAbsent(npc.getIndex(), index -> new TrackedNpc(
			npc.getId(),
			npc.getIndex(),
			npc.getName()
		)).applyHitsplat(e.getHitsplat());
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		int size = trackedNpcs.size();
		trackedNpcs.values().removeIf(trackedNpc -> trackedNpc.getLastSeen()
			.map(lastSeenAt ->
				Duration.between(lastSeenAt, Instant.now()).getSeconds() >= YEET_TIME)
			.orElse(false));
		if(size > trackedNpcs.size())
		{
			log.debug("Yeeted {} NPCs", size - trackedNpcs.size());
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();
		trackedNpcs.computeIfPresent(npc.getIndex(), (index, trackedNpc) -> {
			trackedNpc.setMissing(false);
			return trackedNpc;
		});
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();

		trackedNpcs.computeIfPresent(npc.getIndex(), (index, trackedNpc) -> {
			if (npc.isDead() && trackedNpc.isMyKill())
			{
				logKill(trackedNpc);
				return null; //Remove the NPC if we killed it
			}
			else
			{
				trackedNpc.setMissing(true);
			}
			return trackedNpc; //Keep the little shit if we didn't
		});
	}

	@Subscribe
	private void onActorDeath(ActorDeath e)
	{
		if (!(e.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) e.getActor();
		trackedNpcs.computeIfPresent(npc.getIndex(), (index, trackedNpc) -> {
			if (npc.isDead() && trackedNpc.isMyKill())
			{
				logKill(trackedNpc);
			}
			return null; //Actor death should always result in removal from tracking.
		});
	}

	private void logKill(@NonNull TrackedNpc npc)
	{
		getQueue().addItem(npc);
		log.debug("Logged kill for {} {}", npc.getId(), npc.getName());
	}
}
