package com.ironclad.clangoals.components.tracking;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.api.Endpoint;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.util.BatchQueue;
import com.ironclad.clangoals.util.WorldUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

@Slf4j
@Singleton
public class ItemTrackingComponent extends AbstractTrackingComponent<NpcLootReceived>
{
	private final Client client;
	private final BatchQueue<NpcLootReceived> queue = new BatchQueue<>(this::onFlush);
	;
	private final ItemManager itemManager;
	@Inject

	public ItemTrackingComponent(ApiService api, EventBus eventBus, Client client, ItemManager itemManager)
	{
		super(BatchConfig.Type.ITEM, api, eventBus);
		this.client = client;
		this.itemManager = itemManager;
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return super.isEnabled(config, state) && state.isInEnabledWorld();
	}

	protected void onFlush(List<NpcLootReceived> items)
	{
		List<ItemStack> allTheThings = items.stream()
			.flatMap(e -> e.getItems().stream())
			.collect(Collectors.toList());

		this.api.batchUpdate(Endpoint.LOOT, allTheThings, (item) ->
		{
			JsonObject tmp = new JsonObject();
			tmp.addProperty("item_id", item.getId());
			tmp.addProperty("quantity", item.getQuantity());
			tmp.addProperty("name", itemManager.getItemComposition(item.getId()).getName());
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
				this.queue.flush();
				break;
		}
	}

	@Subscribe
	private void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		if (npcLootReceived.getItems().isEmpty() || WorldUtils.isDisabledWorldType(client.getWorldType()))
		{
			return;
		}
		// Check that player is not within a restricted region.
		if (!WorldUtils.isPlayerWithinMapRegion(client, WorldUtils.LAST_MAN_STANDING_REGIONS) &&
			!WorldUtils.isPlayerWithinMapRegion(client, WorldUtils.SOUL_WARS_REGIONS)
		)
		{
			queue.addItem(npcLootReceived);
		}
	}

}
