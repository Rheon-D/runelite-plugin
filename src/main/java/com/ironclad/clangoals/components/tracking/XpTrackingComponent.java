package com.ironclad.clangoals.components.tracking;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.EnumMap;

@Slf4j
@Singleton
public class XpTrackingComponent extends AbstractTrackingComponent<StatChanged>
{
	private final EnumMap<Skill, Integer> xpMap;
	private final String endpoint;
	private final Client client;
	private boolean requiresInit = false;

	@Inject
	public XpTrackingComponent(ApiService api, EventBus eventBus, @Named("api.endpoint.batch.xp") String endpoint, Client client)
	{
		super(BatchConfig.Type.XP, api, eventBus);
		this.client = client;
		this.xpMap = new EnumMap<>(Skill.class);
		this.endpoint = endpoint;
	}

	@Override
	protected void onComponentStart(PluginState state)
	{
		requiresInit = true;
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		super.onComponentStop(state);
	}

	protected void onFlush(List<StatChanged> items){
		log.debug("Flushing Xp Queue");
		this.api.batchUpdate(endpoint, items, (item) ->
		{
			JsonObject tmp = new JsonObject();
			tmp.addProperty("skill", item.getSkill().getName().toLowerCase());
			tmp.addProperty("xp", item.getXp());

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
			case LOGGING_IN:
				requiresInit = true;
				break;
		}
	}

	@Subscribe
	private void onGameTick(GameTick e){
		if(requiresInit){
			log.debug("Initiating Xp Tracking");
			xpMap.clear();
			for(Skill skill : Skill.values()){
				xpMap.put(skill, client.getSkillExperience(skill));
			}
			requiresInit = false;
		}
	}

	@Subscribe
	private void onStatChanged(StatChanged e)
	{
		if(blockTracking() || requiresInit) return;

		Skill skill = e.getSkill();

		int newXp = e.getXp();
		int oldXp = xpMap.getOrDefault(skill, 0);
		int diff = newXp - oldXp;

		if (diff <= 0)
		{
			//Xp is going backwards or is the same, ignore
			return;
		}
		log.debug("Xp added: {} XP: {}", skill.getName(), newXp);
		getQueue().addItem(e);
		xpMap.put(skill, newXp);
	}
}
