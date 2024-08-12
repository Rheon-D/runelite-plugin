package com.ironclad.clangoals.components.tracking.npcs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.TrackingConfig;
import com.ironclad.clangoals.components.tracking.AbstractTrackingComponent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import static net.runelite.api.NpcID.ABYSSAL_SIRE;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5887;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5888;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5889;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5890;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5891;
import static net.runelite.api.NpcID.ABYSSAL_SIRE_5908;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8616;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8617;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8618;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8619;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8620;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8621;
import static net.runelite.api.NpcID.ALCHEMICAL_HYDRA_8622;
import static net.runelite.api.NpcID.ARTIO;
import static net.runelite.api.NpcID.BRYOPHYTA;
import static net.runelite.api.NpcID.CALLISTO;
import static net.runelite.api.NpcID.CALLISTO_6609;
import static net.runelite.api.NpcID.CALVARION;
import static net.runelite.api.NpcID.CALVARION_11994;
import static net.runelite.api.NpcID.CALVARION_11995;
import static net.runelite.api.NpcID.CERBERUS;
import static net.runelite.api.NpcID.CERBERUS_5863;
import static net.runelite.api.NpcID.CERBERUS_5866;
import static net.runelite.api.NpcID.CHAOS_ELEMENTAL;
import static net.runelite.api.NpcID.CHAOS_ELEMENTAL_6505;
import static net.runelite.api.NpcID.CHAOS_FANATIC;
import static net.runelite.api.NpcID.COMMANDER_ZILYANA;
import static net.runelite.api.NpcID.COMMANDER_ZILYANA_6493;
import static net.runelite.api.NpcID.CORPOREAL_BEAST;
import static net.runelite.api.NpcID.CRAZY_ARCHAEOLOGIST;
import static net.runelite.api.NpcID.DAGANNOTH_PRIME;
import static net.runelite.api.NpcID.DAGANNOTH_PRIME_6497;
import static net.runelite.api.NpcID.DAGANNOTH_REX;
import static net.runelite.api.NpcID.DAGANNOTH_REX_6498;
import static net.runelite.api.NpcID.DAGANNOTH_SUPREME;
import static net.runelite.api.NpcID.DAGANNOTH_SUPREME_6496;
import static net.runelite.api.NpcID.DAWN;
import static net.runelite.api.NpcID.DAWN_7852;
import static net.runelite.api.NpcID.DAWN_7853;
import static net.runelite.api.NpcID.DERANGED_ARCHAEOLOGIST;
import static net.runelite.api.NpcID.DUKE_SUCELLUS;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12167;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12191;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12192;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12193;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12194;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12195;
import static net.runelite.api.NpcID.DUKE_SUCELLUS_12196;
import static net.runelite.api.NpcID.DUSK;
import static net.runelite.api.NpcID.DUSK_7851;
import static net.runelite.api.NpcID.DUSK_7854;
import static net.runelite.api.NpcID.DUSK_7855;
import static net.runelite.api.NpcID.GENERAL_GRAARDOR;
import static net.runelite.api.NpcID.GENERAL_GRAARDOR_6494;
import static net.runelite.api.NpcID.GIANT_MOLE;
import static net.runelite.api.NpcID.GIANT_MOLE_6499;
import static net.runelite.api.NpcID.KALPHITE_QUEEN;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_4303;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_4304;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_6500;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_6501;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_963;
import static net.runelite.api.NpcID.KALPHITE_QUEEN_965;
import static net.runelite.api.NpcID.KING_BLACK_DRAGON;
import static net.runelite.api.NpcID.KING_BLACK_DRAGON_2642;
import static net.runelite.api.NpcID.KING_BLACK_DRAGON_6502;
import static net.runelite.api.NpcID.KRAKEN;
import static net.runelite.api.NpcID.KRAKEN_6640;
import static net.runelite.api.NpcID.KRAKEN_6656;
import static net.runelite.api.NpcID.KREEARRA;
import static net.runelite.api.NpcID.KREEARRA_6492;
import static net.runelite.api.NpcID.KRIL_TSUTSAROTH;
import static net.runelite.api.NpcID.KRIL_TSUTSAROTH_6495;
import static net.runelite.api.NpcID.NEX;
import static net.runelite.api.NpcID.NEX_11279;
import static net.runelite.api.NpcID.NEX_11280;
import static net.runelite.api.NpcID.NEX_11281;
import static net.runelite.api.NpcID.NEX_11282;
import static net.runelite.api.NpcID.OBOR;
import static net.runelite.api.NpcID.PHANTOM_MUSPAH;
import static net.runelite.api.NpcID.PHANTOM_MUSPAH_12078;
import static net.runelite.api.NpcID.PHANTOM_MUSPAH_12079;
import static net.runelite.api.NpcID.PHANTOM_MUSPAH_12080;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_11153;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_11154;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_11155;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9417;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9418;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9419;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9420;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9421;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9422;
import static net.runelite.api.NpcID.PHOSANIS_NIGHTMARE_9424;
import static net.runelite.api.NpcID.SARACHNIS;
import static net.runelite.api.NpcID.SCORPIA;
import static net.runelite.api.NpcID.SCURRIUS;
import static net.runelite.api.NpcID.SCURRIUS_7222;
import static net.runelite.api.NpcID.SKOTIZO;
import static net.runelite.api.NpcID.SPINDEL;
import static net.runelite.api.NpcID.THERMONUCLEAR_SMOKE_DEVIL;
import static net.runelite.api.NpcID.THE_LEVIATHAN;
import static net.runelite.api.NpcID.THE_LEVIATHAN_12215;
import static net.runelite.api.NpcID.THE_LEVIATHAN_12219;
import static net.runelite.api.NpcID.THE_LEVIATHAN_12591;
import static net.runelite.api.NpcID.THE_MIMIC;
import static net.runelite.api.NpcID.THE_MIMIC_8633;
import static net.runelite.api.NpcID.THE_NIGHTMARE;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9426;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9427;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9428;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9429;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9430;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9431;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9432;
import static net.runelite.api.NpcID.THE_NIGHTMARE_9433;
import static net.runelite.api.NpcID.THE_WHISPERER;
import static net.runelite.api.NpcID.THE_WHISPERER_12205;
import static net.runelite.api.NpcID.THE_WHISPERER_12206;
import static net.runelite.api.NpcID.THE_WHISPERER_12207;
import static net.runelite.api.NpcID.VARDORVIS;
import static net.runelite.api.NpcID.VARDORVIS_12224;
import static net.runelite.api.NpcID.VARDORVIS_12228;
import static net.runelite.api.NpcID.VARDORVIS_12425;
import static net.runelite.api.NpcID.VARDORVIS_12426;
import static net.runelite.api.NpcID.VARDORVIS_13656;
import static net.runelite.api.NpcID.VENENATIS;
import static net.runelite.api.NpcID.VENENATIS_6610;
import static net.runelite.api.NpcID.VETION;
import static net.runelite.api.NpcID.VETION_6612;
import static net.runelite.api.NpcID.VORKATH;
import static net.runelite.api.NpcID.VORKATH_8058;
import static net.runelite.api.NpcID.VORKATH_8059;
import static net.runelite.api.NpcID.VORKATH_8060;
import static net.runelite.api.NpcID.VORKATH_8061;
import static net.runelite.api.NpcID.ZALCANO;
import static net.runelite.api.NpcID.ZALCANO_9050;
import static net.runelite.api.NpcID.ZULRAH;
import static net.runelite.api.NpcID.ZULRAH_2043;
import static net.runelite.api.NpcID.ZULRAH_2044;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.NPCManager;
import net.runelite.client.util.Text;


/*
 * Welcome to the scuff deluxe
 *
 * Bosses will only be tracked by the loot event.
 * This is to avoid me using my brain and in case there is some edge case or mechanic that stuffs up the tracking.
 *
 * Other NPCs will be tracked by hitsplats. If our damage exceeds others and is 50% of the npc health,
 * we count the kill as ours.
 *
 * On a hitsplat, NPC is added to the tracking list.
 * On a despawn event TrackedNpcs that are not dead will be marked missing, If the NPC is not seen for X ticks, it is removed.
 * On a spawn event, if the NPC is marked missing, it is unmarked.
 */
@Slf4j
@Singleton
public class NPCTrackingComponent extends AbstractTrackingComponent<TrackedNpc>
{
	private static final Set<Integer> BOSS_IDS = ImmutableSet.of(
		ABYSSAL_SIRE, ABYSSAL_SIRE_5887, ABYSSAL_SIRE_5888, ABYSSAL_SIRE_5889, ABYSSAL_SIRE_5890, ABYSSAL_SIRE_5891, ABYSSAL_SIRE_5908,
		ALCHEMICAL_HYDRA, ALCHEMICAL_HYDRA_8616, ALCHEMICAL_HYDRA_8617, ALCHEMICAL_HYDRA_8618, ALCHEMICAL_HYDRA_8619, ALCHEMICAL_HYDRA_8620, ALCHEMICAL_HYDRA_8621, ALCHEMICAL_HYDRA_8622,
		ARTIO,
		BRYOPHYTA,
		CALLISTO, CALLISTO_6609,
		CALVARION, CALVARION_11994, CALVARION_11995,
		CERBERUS, CERBERUS_5863, CERBERUS_5866,
		CHAOS_ELEMENTAL, CHAOS_ELEMENTAL_6505,
		CHAOS_FANATIC,
		COMMANDER_ZILYANA, COMMANDER_ZILYANA_6493,
		CORPOREAL_BEAST,
		CRAZY_ARCHAEOLOGIST,
		DAGANNOTH_PRIME, DAGANNOTH_PRIME_6497,
		DAGANNOTH_REX, DAGANNOTH_REX_6498,
		DAGANNOTH_SUPREME, DAGANNOTH_SUPREME_6496,
		DAWN, DAWN_7852, DAWN_7853,
		DERANGED_ARCHAEOLOGIST,
		DUKE_SUCELLUS, DUKE_SUCELLUS_12167, DUKE_SUCELLUS_12191, DUKE_SUCELLUS_12192, DUKE_SUCELLUS_12193,/*Quest*/DUKE_SUCELLUS_12194,/*Quest*/DUKE_SUCELLUS_12195,/*Quest*/DUKE_SUCELLUS_12196,
		DUSK, DUSK_7851, DUSK_7854, DUSK_7855,
		GENERAL_GRAARDOR, GENERAL_GRAARDOR_6494,
		GIANT_MOLE, GIANT_MOLE_6499,
		KALPHITE_QUEEN, KALPHITE_QUEEN_4303, KALPHITE_QUEEN_4304, KALPHITE_QUEEN_6500, KALPHITE_QUEEN_6501, KALPHITE_QUEEN_963, KALPHITE_QUEEN_965,
		KING_BLACK_DRAGON, KING_BLACK_DRAGON_2642, KING_BLACK_DRAGON_6502,
		KRAKEN, KRAKEN_6640, KRAKEN_6656,
		KREEARRA, KREEARRA_6492,
		KRIL_TSUTSAROTH, KRIL_TSUTSAROTH_6495,
		NEX, NEX_11279, NEX_11280, NEX_11281, NEX_11282,
		OBOR,
		PHANTOM_MUSPAH, PHANTOM_MUSPAH_12078, PHANTOM_MUSPAH_12079, PHANTOM_MUSPAH_12080,
		PHOSANIS_NIGHTMARE, PHOSANIS_NIGHTMARE_11153, PHOSANIS_NIGHTMARE_11154, PHOSANIS_NIGHTMARE_11155, PHOSANIS_NIGHTMARE_9417, PHOSANIS_NIGHTMARE_9418, PHOSANIS_NIGHTMARE_9419, PHOSANIS_NIGHTMARE_9420, PHOSANIS_NIGHTMARE_9421, PHOSANIS_NIGHTMARE_9422, PHOSANIS_NIGHTMARE_9424,
		SARACHNIS,
		SCORPIA,
		SCURRIUS, SCURRIUS_7222,
		SKOTIZO,
		SPINDEL,
		THERMONUCLEAR_SMOKE_DEVIL,
		THE_LEVIATHAN, THE_LEVIATHAN_12215,/*Quest*/THE_LEVIATHAN_12219,/*Quest*/THE_LEVIATHAN_12591,
		THE_MIMIC, THE_MIMIC_8633,
		THE_NIGHTMARE, THE_NIGHTMARE_9426, THE_NIGHTMARE_9427, THE_NIGHTMARE_9428, THE_NIGHTMARE_9429, THE_NIGHTMARE_9430, THE_NIGHTMARE_9431, THE_NIGHTMARE_9432, THE_NIGHTMARE_9433,
		THE_WHISPERER, THE_WHISPERER_12205, THE_WHISPERER_12206, THE_WHISPERER_12207,
		VARDORVIS, VARDORVIS_12224,/*Quest*/VARDORVIS_12228,/*Quest*/VARDORVIS_12425,/*Quest*/VARDORVIS_12426, VARDORVIS_13656,
		VENENATIS, VENENATIS_6610,
		VETION, VETION_6612,
		VORKATH, VORKATH_8058, VORKATH_8059, VORKATH_8060, VORKATH_8061,
		ZALCANO, ZALCANO_9050,
		ZULRAH, ZULRAH_2043, ZULRAH_2044
	);
	static final int MISSING_DELAY_TICKS = 50;

	@Getter
	private final Map<Integer, TrackedNpc> trackedNpcs;
	private final String endpoint;
	private final NPCManager npcManager;

	@Inject
	public NPCTrackingComponent(ApiService api,
								EventBus eventBus,
								@Named("api.endpoint.batch.npc") String endpoint,
								ScheduledExecutorService executor,
								NPCManager npcManager)
	{
		super(TrackingConfig.Type.NPC, api, eventBus, executor);
		this.endpoint = endpoint;
		trackedNpcs = Maps.newHashMap();
		this.npcManager = npcManager;
	}

	@Override
	protected void onComponentStart(PluginState state)
	{
		//Nothing interesting happens
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		trackedNpcs.clear();
		getQueue().flush();
	}

	@Override
	protected void onFlush(List<TrackedNpc> pluginNPCS)
	{
		log.debug("Flushing Npc Queue");
		this.api.batchUpdateAsync(endpoint, pluginNPCS, (item) -> {
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

	@Subscribe
	private void onNpcLootReceived(NpcLootReceived e)
	{
		if (blockTracking())
		{
			return;
		}

		NPC npc = e.getNpc();
		boolean isBoss = BOSS_IDS.contains(npc.getId());

		if (isBoss)
		{
			TrackedNpc t = createTrackedNpc(npc);
			if (t != null)
			{
				logKill(t);
			}
			else
			{
				log.debug("Failed to create TrackedNpc for npc: {}", npc);
			}
		}
		else
		{
			trackedNpcs.computeIfPresent(npc.getIndex(), (i, n) -> {
				logKill(n);
				return null;
			});
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned e)
	{
		if (blockTracking())
		{
			return;
		}

		trackedNpcs.computeIfPresent(e.getNpc().getIndex(), (i, n) -> {
			if (n.isMissing())
			{
				n.setMissing(false);
			}
			return n;
		});
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned e)
	{
		if (blockTracking())
		{
			return;
		}

		trackedNpcs.computeIfPresent(e.getNpc().getIndex(), (i, npc) -> {
			if (!e.getNpc().isDead())
			{
				npc.setMissing(true);
				return npc;
			}

			if (npc.isMyKill())
			{
				logKill(npc);
			}
			return null;
		});
	}

	@Subscribe
	private void onHitsplatApplied(HitsplatApplied e)
	{
		if (blockTracking())
		{
			return;
		}

		if (e.getActor() instanceof NPC)
		{
			NPC npc = (NPC) e.getActor();
			boolean isBoss = BOSS_IDS.contains(npc.getId());

			if (isBoss)
			{
				//Bosses are tracked by loot event.
				return;
			}

			TrackedNpc t = trackedNpcs.computeIfAbsent(npc.getIndex(), (i) -> {
				TrackedNpc trackedNpc = createTrackedNpc(npc);
				if (trackedNpc != null)
				{
					trackedNpc.applyHitsplat(e.getHitsplat());
				}
				return trackedNpc;
			});

			if (t != null)
			{
				t.applyHitsplat(e.getHitsplat());
			}
		}
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		trackedNpcs.entrySet().removeIf(entry -> {
			TrackedNpc npc = entry.getValue();
			return npc.isMissing() && npc.isTimedOut();
		});
	}

	private TrackedNpc createTrackedNpc(NPC npc)
	{
		String name = getName(npc);
		int index = npc.getIndex();
		Integer health = npcManager.getHealth(npc.getId());

		if (name == null || health == null)
		{
			return null;
		}

		return new TrackedNpc(index, npc.getId(), health, name);
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
		getQueue().addItem(npc);
		log.debug("Logged Kill: {}", npc);
	}
}
