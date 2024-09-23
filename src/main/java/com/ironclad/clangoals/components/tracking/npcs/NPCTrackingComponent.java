package com.ironclad.clangoals.components.tracking.npcs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.AbstractTrackingComponent;
import com.ironclad.clangoals.util.Region;
import com.ironclad.clangoals.util.WorldUtils;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.util.RSTimeUnit;
import net.runelite.client.util.Text;


/*
 * Welcome to the scuff deluxe
 *
 * As accurate as willing, ie As accurate as my brain is, ie not very.
 * The idea is a npc is tracked via hitsplat -> despawn -> is dead.
 * If the NPC is not dead, we will mark it as missing and remove it after a timeout.
 * If the NPC is dead, and we meet credit criteria, we will log the kill.
 * If the NPC is added to the loot only list we will fall back to the loot event, this will time out.
 *  - We can assume we got the kill if we get the loot.
 * That last part is to avoid me using my brain.
 *
 * Lastly some NPCs are tracked via chat messages, we can avoid ingame mechanics for bosses by tracking the kc message.
 * Also, if we are super lucky a boss may not actually drop anything *cough* Sarachnis *cough*.
 *
 * Other NPCs will be tracked by our hitsplats. If we meet criteria, we will be credited with the kill.
 *
 * On a hitsplat, NPC is added to the tracking list.
 * On a despawn event TrackedNpcs that are not dead will be marked missing, If the NPC is not seen for X time, it is removed.
 * On a spawn event, if the NPC is marked missing, it is unmarked.
 *
 * Event order goes: Death -> {Despawn -> Loot (if applicable)} can be same tick.
 */
@Slf4j
@Singleton
public class NPCTrackingComponent extends AbstractTrackingComponent<TrackedNpc, NPCTrackingConfig>
{
	private static final EnumSet<ChatMessageType> ALLOWED_CHAT_TYPES = EnumSet.of(ChatMessageType.GAMEMESSAGE, ChatMessageType.SPAM);
	private static final EnumSet<Region> RAID_REGIONS = EnumSet.of(Region.CHAMBERS_OF_XERIC, Region.THEATRE_OF_BLOOD, Region.TOMES_OF_AMASCUT);
	private static final EnumSet<Region> MINIGAME_REGIONS = EnumSet.of(Region.SOUL_WARS, Region.TEMPOROSS, Region.ZALCANO, Region.WINTERTODT, Region.VOLCANIC_MINE);
	private static final EnumSet<Region> DISABLED_REGIONS = EnumSet.of(Region.NIGHTMARE_ZONE, Region.CASTLE_WARS, Region.PVP_ARENA);
	static final Duration MISSING_DELAY_TICKS = Duration.of(50, RSTimeUnit.GAME_TICKS);

	@Getter
	private final Map<Integer, TrackedNpc> trackedNpcs;
	private final Map<Integer, TrackedNpc> lootOnlyNpcs;
	private final Map<String, Mapping> chatMappings;
	private final Set<Integer> chatNpcs;
	private final String endpoint;
	private final Client client;


	@Inject
	public NPCTrackingComponent(ApiService api,
								EventBus eventBus,
								@Named("api.endpoint.batch.npc") String endpoint,
								ScheduledExecutorService executor,
								Client client,
								RemoteConfig remoteConfig)
	{
		super(api, eventBus, executor, remoteConfig);
		this.endpoint = endpoint;
		this.trackedNpcs = Maps.newHashMap();
		this.lootOnlyNpcs = Maps.newHashMap();
		this.chatMappings = Maps.newHashMap();
		this.chatNpcs = Sets.newHashSet();
		this.client = client;
	}

	@Override
	protected void onComponentStart(PluginState state)
	{
		rebuild(getConfig());
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		this.chatMappings.clear();
		this.chatNpcs.clear();
		this.trackedNpcs.clear();
		this.lootOnlyNpcs.clear();
		getQueue().flush();
	}

	@Override
	protected void onFlush(List<TrackedNpc> pluginNPCS)
	{
		log.debug("Flushing Npc Queue");
		this.api.batchUpdateAsync(this.endpoint, pluginNPCS, (item) -> {
			JsonObject tmp = new JsonObject();
			tmp.addProperty("npc_id", item.getId());
			tmp.addProperty("name", item.getName());
			return tmp;
		});
	}

	@Override
	protected void rebuild(NPCTrackingConfig config)
	{
		this.chatMappings.clear();
		this.chatNpcs.clear();
		this.chatMappings.putAll(config.getMappings().stream().collect(Collectors.toMap(Mapping::getName, m -> m)));
		this.chatNpcs.addAll(config.getMappings().stream().flatMap(m -> m.getBlacklist().stream()).collect(Collectors.toSet()));
	}


	@Override
	protected boolean componentEnabled(IroncladClanGoalsConfig config)
	{
		return config.enableNpcTracking();
	}

	@Override
	protected NPCTrackingConfig getConfig()
	{
		return this.rConf.getNpcTrackingConfig();
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				getQueue().flush();
				this.trackedNpcs.clear();
				break;
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage e)
	{
		if (!ALLOWED_CHAT_TYPES.contains(e.getType())
			|| WorldUtils.inRegion(this.client, DISABLED_REGIONS))
		{
			return;
		}

		String message = e.getMessage();

		for (MessageMatcher matcher : getConfig().getMessageMatchers())
		{
			matcher.apply(message).ifPresent(res -> {
				String s = Text.removeTags(res);
				if (!Strings.isNullOrEmpty(s))
				{
					Mapping mapping = this.chatMappings.get(s);
					if (mapping != null)
					{
						logKill(TrackedNpc.builder()
							.index(-1)
							.id(mapping.getId())
							.name(mapping.getName())
							.creditMethod(TrackedNpc.CreditMethod.ALWAYS)
							.build());
					}
				}
			});

		}
	}

	@Subscribe
	private void onNpcLootReceived(NpcLootReceived e)
	{
		NPC npc = e.getNpc();

		this.trackedNpcs.computeIfPresent(npc.getIndex(), (i, tn) -> {
			logKill(tn);
			log.debug("NPC {}-{} looted", tn.getIndex(), tn.getName());
			return null;
		});

		this.lootOnlyNpcs.computeIfPresent(npc.getIndex(), (i, tn) -> {
			logKill(tn);
			log.debug("Loot only NPC {}-{} looted", tn.getIndex(), tn.getName());
			return null;
		});
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned e)
	{
		this.trackedNpcs.computeIfPresent(e.getNpc().getIndex(), (i, tn) -> {
			tn.setMissing(false);
			log.debug("Spawned NPC {}-{} marked not missing", tn.getIndex(), tn.getName());
			return tn;
		});
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned e)
	{
		this.trackedNpcs.computeIfPresent(e.getNpc().getIndex(), (i, tn) -> {
			if (!e.getNpc().isDead())
			{
				tn.setMissing(true);
				log.debug("Despawned NPC {}-{} marked missing", tn.getIndex(), tn.getName());
				return tn;
			}

			if (isLootNpc(tn.getId()))
			{
				tn.setMissing(true);
				this.lootOnlyNpcs.put(i, tn);
				log.debug("Queued despawned NPC {} for loot only tracking", tn.getName());
			}
			else if (tn.creditMe())
			{
				logKill(tn);
			}

			return null;
		});
	}

	@Subscribe
	private void onActorDeath(ActorDeath e)
	{
		if (e.getActor() instanceof NPC)
		{
			NPC npc = (NPC) e.getActor();

			this.trackedNpcs.computeIfPresent(npc.getIndex(), (i, tn) -> {
				log.debug("NPC {} died", tn.getName());
				if (isLootNpc(tn.getId()))
				{
					tn.setMissing(true);
					this.lootOnlyNpcs.put(i, tn);
					log.debug("Queued dead NPC {} for loot only tracking", tn.getName());
				}
				else if (tn.creditMe())
				{
					logKill(tn);
				}

				return null;
			});
		}
	}

	@Subscribe
	private void onNpcChanged(NpcChanged e)
	{
		this.trackedNpcs.computeIfPresent(e.getNpc().getIndex(), (i, tn) -> {
			tn.setId(e.getNpc().getId());
			log.debug("NPC {} changed {} to {}", tn.getName(), e.getOld().getId(), e.getNpc().getId());
			return tn;
		});
	}

	@Subscribe
	private void onHitsplatApplied(HitsplatApplied e)
	{
		if (e.getActor() instanceof NPC)
		{
			NPC npc = (NPC) e.getActor();
			int region = WorldUtils.getPlayerRegion(this.client);
			if (isChatNpc(npc.getId())
				|| WorldUtils.inRegion(region, MINIGAME_REGIONS)
				|| WorldUtils.inRegion(region, DISABLED_REGIONS))
			{
				return;
			}

			TrackedNpc tn = this.trackedNpcs.computeIfAbsent(npc.getIndex(), (i) -> createTrackedNpc(npc));

			if (tn != null)
			{
				tn.applyHitsplat(e.getHitsplat());
			}
		}
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		this.trackedNpcs.entrySet().removeIf(entry -> {
			TrackedNpc npc = entry.getValue();
			return npc.isMissing() && npc.isTimedOut();
		});

		this.lootOnlyNpcs.entrySet().removeIf(entry -> {
			TrackedNpc npc = entry.getValue();
			return npc.isMissing() && npc.isTimedOut();
		});
	}

	boolean isLootNpc(int id)
	{
		return getConfig().getLootOnlyNpcs().contains(id);
	}

	boolean isChatNpc(int id)
	{
		return this.chatNpcs.contains(id);
	}

	private TrackedNpc createTrackedNpc(NPC npc)
	{
		String name = getName(npc);
		boolean inRaid = WorldUtils.inRegion(this.client, RAID_REGIONS);

		if (Strings.isNullOrEmpty(name))
		{
			return null;
		}

		if (inRaid && !getConfig().whitelistedRaidNpcs.contains(name))
		{
			return null;
		}

		log.debug("Creating TrackedNpc: {}-{}-{}", npc.getIndex(), npc.getId(), name);

		return TrackedNpc.builder()
			.index(npc.getIndex())
			.id(npc.getId())
			.name(name)
			.creditMethod(inRaid ? TrackedNpc.CreditMethod.CONTRIBUTED : TrackedNpc.CreditMethod.ONLY_DAMAGE)
			.build();
	}

	private String getName(@NonNull NPC npc)
	{
		//Probably entirely unnecessary. But It threw null once and IDK why.
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		String name = composition == null ? npc.getName() : composition.getName();
		return Strings.isNullOrEmpty(name) ? name : Text.removeTags(name.replace('\u00A0', ' '));
	}

	private void logKill(TrackedNpc npc)
	{
		if (blockTracking() || npc == null)
		{
			return;
		}

		getQueue().addItem(npc);
		log.debug("Logged Kill: {}", npc);
	}
}
