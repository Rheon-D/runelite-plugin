package com.ironclad.clangoals.components.tracking;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.components.service.api.Endpoint;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.EnumMap;

@Slf4j
@Singleton
public class XpTrackingComponent extends AbstractTrackingComponent<StatChanged>
{
	private final EnumMap<Skill, Integer> xpMap;

	@Inject
	public XpTrackingComponent(ApiService api, EventBus eventBus)
	{
		super(BatchConfig.Type.XP, api, eventBus);
		this.xpMap = new EnumMap<>(Skill.class);
	}

	protected void onFlush(List<StatChanged> items){
		this.api.batchUpdate(Endpoint.XP, items, (item) ->
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
				break;
			case LOGGED_IN:
				this.xpMap.clear();
				break;
			default:
				//Nothing interesting happens
				break;
		}
	}

	@Subscribe
	private void onStatChanged(StatChanged e)
	{
		if((getState().isInGame() && getState().isInEnabledWorld()))
		{
			return;
		}
		Skill skill = e.getSkill();

		int newXp = e.getXp();
		// We don't want to log skills straightaway
		// as we get flooded with current xp on login.
		if (xpMap.containsKey(skill))
		{
			int oldXp = xpMap.get(skill);
			int diff = newXp - oldXp;

			if (diff <= 0)
			{
				//Xp is going backwards or is the same, ignore
				return;
			}
			log.debug("Xp added: {} XP: {}", skill.getName(), newXp);
		}

		xpMap.put(skill, newXp);
	}
}
