package com.ironclad.clangoals.components.tracking;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.dto.BatchConfig;
import com.ironclad.clangoals.util.WorldUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;

@Slf4j
@Singleton
public class ItemTrackingComponent extends AbstractTrackingComponent<ItemTrackingComponent.Record>
{
	private final Client client;
	private final ItemManager itemManager;

	private final String endpoint;

	@Inject
	public ItemTrackingComponent(ApiService api,
								 EventBus eventBus,
								 Client client,
								 ItemManager itemManager,
								 @Named("api.endpoint.batch.loot") String endpoint)
	{
		super(BatchConfig.Type.ITEM, api, eventBus);
		this.client = client;
		this.itemManager = itemManager;
		this.endpoint = endpoint;
	}

	protected void onFlush(List<Record> items)
	{
		log.debug("Flushing Loot Queue");
		List<ItemData> allTheThings = items.stream()
			.flatMap(e -> e.getData().stream())
			.collect(Collectors.toList());

		this.api.batchUpdate(endpoint, allTheThings, (item) ->
		{
			JsonObject tmp = new JsonObject();
			tmp.addProperty("item_id", item.getItemId());
			tmp.addProperty("quantity", item.getQuantity());
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
	private void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		if (blockTracking() || npcLootReceived.getItems().isEmpty())
		{
			return;
		}
		getQueue().addItem(new Record(npcLootReceived
			.getItems().stream().map(stack -> new ItemData(
				stack.getId(),
				stack.getQuantity(),
				itemManager
					.getItemComposition(
						stack.getId())
					.getName()
			))
			.collect(Collectors.toList())));
	}

	@Override
	protected boolean blockTracking()
	{
		return super.blockTracking()
			|| WorldUtils.isPlayerWithinMapRegion(client, WorldUtils.LAST_MAN_STANDING_REGIONS)
			|| WorldUtils.isPlayerWithinMapRegion(client, WorldUtils.SOUL_WARS_REGIONS);
	}

	@Value
	@NonNull
	static class ItemData
	{
		int itemId;
		int quantity;
		String name;
	}

	@Value
	static class Record
	{
		List<ItemData> data;
	}

}
