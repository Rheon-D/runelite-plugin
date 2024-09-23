package com.ironclad.clangoals.components.tracking.xp;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.AbstractTrackingComponent;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
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
public class XpTrackingComponent extends AbstractTrackingComponent<StatChanged, XpTrackingConfig>
{
	private final EnumMap<Skill, Integer> xpMap;
	private final String endpoint;
	private final Client client;
	private boolean requiresInit = false;

	@Inject
	public XpTrackingComponent(ApiService api,
							   EventBus eventBus,
							   @Named("api.endpoint.batch.xp") String endpoint,
							   Client client,
							   ScheduledExecutorService executor,
							   RemoteConfig rConf)
	{
		super(api, eventBus, executor, rConf);
		this.client = client;
		this.xpMap = new EnumMap<>(Skill.class);
		this.endpoint = endpoint;
	}

	@Override
	protected void onComponentStart(PluginState state)
	{
		this.requiresInit = true;
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		super.onComponentStop(state);
	}

	protected void onFlush(List<StatChanged> items){
		log.debug("Flushing Xp Queue");
		this.api.batchUpdateAsync(this.endpoint, items, (item) ->
		{
			JsonObject tmp = new JsonObject();
			tmp.addProperty("skill", item.getSkill().getName().toLowerCase());
			tmp.addProperty("xp", item.getXp());

			return tmp;
		});
	}

	@Override
	protected boolean componentEnabled(IroncladClanGoalsConfig config)
	{
		return config.enableXpTracking();
	}

	@Override
	protected void rebuild(XpTrackingConfig config)
	{
		//NAAAAAAAA
	}

	@Override
	protected XpTrackingConfig getConfig()
	{
		return this.rConf.getXpTrackingConfig();
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
				this.requiresInit = true;
				break;
		}
	}

	@Subscribe
	private void onGameTick(GameTick e){
		if(this.requiresInit){
			log.debug("Initiating Xp Tracking");
			this.xpMap.clear();
			for(Skill skill : Skill.values()){
				this.xpMap.put(skill, this.client.getSkillExperience(skill));
			}
			this.requiresInit = false;
		}
	}

	@Subscribe
	private void onStatChanged(StatChanged e)
	{
		if(blockTracking() || this.requiresInit) return;

		Skill skill = e.getSkill();

		int newXp = e.getXp();
		int oldXp = this.xpMap.getOrDefault(skill, 0);
		int diff = newXp - oldXp;

		if (diff <= 0)
		{
			//Xp is going backwards or is the same, ignore
			return;
		}
		log.debug("Xp added: {} XP: {}", skill.getName(), newXp);
		getQueue().addItem(e);
		this.xpMap.put(skill, newXp);
	}
}
