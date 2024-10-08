package com.ironclad.clangoals.components.tracking.loot;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.components.service.PluginState;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.service.config.RemoteConfigChanged;
import com.ironclad.clangoals.components.tracking.AbstractTrackingComponent;
import com.ironclad.clangoals.util.Region;
import com.ironclad.clangoals.util.WorldUtils;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
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
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

@Slf4j
@Singleton
public class ItemTrackingComponent extends AbstractTrackingComponent<ItemTrackingComponent.Record, ItemTrackingConfig>
{
	private static final Set<Integer> BLOCKED_REGIONS = Region.combine(Region.LAST_MAN_STANDING, Region.SOUL_WARS); //TODO move to remote config
	private final Client client;
	private final ItemManager itemManager;
	private final String endpoint;
	private final Table<LootRecordType, String, Mapping> table;

	@Inject
	public ItemTrackingComponent(ApiService api,
								 EventBus eventBus,
								 Client client,
								 ItemManager itemManager,
								 @Named("api.endpoint.batch.loot") String endpoint,
								 RemoteConfig rConf,
								 ScheduledExecutorService executor)
	{
		super(api, eventBus, executor, rConf);
		this.client = client;
		this.itemManager = itemManager;
		this.endpoint = endpoint;
		this.table = HashBasedTable.create();
	}

	protected void onFlush(List<Record> items)
	{
		log.debug("Flushing Item Queue");
		List<ItemData> allTheThings = items.stream()
			.flatMap(e -> e.getData().stream())
			.collect(Collectors.toList());

		this.api.batchUpdateAsync(this.endpoint, allTheThings, (item) ->
		{
			JsonObject tmp = new JsonObject();
			tmp.addProperty("item_id", item.getItemId());
			tmp.addProperty("quantity", item.getQuantity());
			tmp.addProperty("name", item.getName());
			return tmp;
		});
	}

	@Override
	protected boolean componentEnabled(IroncladClanGoalsConfig config)
	{
		return config.enableItemTracking();
	}

	@Override
	protected void onComponentStart(PluginState state)
	{
		rebuild(getConfig());
	}

	@Override
	protected void onComponentStop(PluginState state)
	{
		this.table.clear();
	}

	@Override
	protected void rebuild(ItemTrackingConfig config)
	{
		this.table.clear();

		config.getLootEvents().forEach((item) -> this.table.put(item.getType(), item.getEvent(), item));
	}

	@Override
	protected ItemTrackingConfig getConfig()
	{
		return this.rConf.getItemTrackingConfig();
	}

	@Subscribe
	private void onRemoteConfigChanged(RemoteConfigChanged e)
	{
		rebuild(getConfig());
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
	private void onLootReceived(LootReceived e)
	{
		if (blockTracking()
			|| e.getItems().isEmpty())
		{
			return;
		}

		Mapping mapping = this.table.get(e.getType(), e.getName());
		if (Objects.nonNull(mapping))
		{
			ItemData eventItem = new ItemData(mapping.getId(), 1, mapping.getEvent());
			List<ItemData> items = toItemData(e.getItems());
			items.add(eventItem);
			logItems(items);
		}
	}

	@Subscribe
	private void onNpcLootReceived(NpcLootReceived e)
	{
		if (blockTracking() || e.getItems().isEmpty())
		{
			return;
		}

		logItems(e.getItems());
	}

	private void logItems(@NonNull Collection<ItemStack> items)
	{
		logItems(toItemData(items));
	}

	private void logItems(@NonNull List<ItemData> items)
	{
		getQueue().addItem(new Record(items));
	}

	private List<ItemData> toItemData(Collection<ItemStack> items)
	{
		return items.stream()
			.filter(Objects::nonNull)
			.map(stack -> new ItemData(
				stack.getId(),
				stack.getQuantity(),
				this.itemManager
					.getItemComposition(stack.getId())
					.getName()
			))
			.collect(Collectors.toList());
	}

	@Override
	protected boolean blockTracking()
	{
		return super.blockTracking()
			|| WorldUtils.inRegion(this.client, BLOCKED_REGIONS);
	}

	@Value
	static class ItemData
	{
		int itemId;
		int quantity;
		@NonNull String name;
	}

	@Value
	static class Record
	{
		List<ItemData> data;
	}
}
