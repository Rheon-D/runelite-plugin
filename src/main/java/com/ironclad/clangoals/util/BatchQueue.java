package com.ironclad.clangoals.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.util.RSTimeUnit;

@Slf4j
public final class BatchQueue<T>
{
	private static final int DEFAULT_MAX_ITEMS = 10;
	private static final long DEFAULT_FLUSH = RSTimeUnit.GAME_TICKS.getDuration().toMillis() * 15;
	private ScheduledExecutorService executor;
	private final ConcurrentLinkedQueue<T> queue;
	private final int limit;
	private AtomicInteger itemCount;
	private final long cooldown;
	private final Consumer<List<T>> onFlush;

	/**
	 * Construct a new BatchQueue with default values.
	 */
	public BatchQueue(Consumer<List<T>> onFlush)
	{
		this(DEFAULT_MAX_ITEMS, DEFAULT_FLUSH, onFlush);
	}

	public BatchQueue(int itemLimit, Consumer<List<T>> onFlush)
	{
		this(itemLimit, DEFAULT_FLUSH, onFlush);
	}

	public BatchQueue(long cooldown, Consumer<List<T>> onFlush)
	{
		this(DEFAULT_MAX_ITEMS, cooldown, onFlush);
	}

	/**
	 * @param itemLimit Flush when items reach this amount. -1 to disable.
	 * @param cooldown  MS, Flush when cooldown is reached.
	 */
	public BatchQueue(int itemLimit, long cooldown, Consumer<List<T>> onFlush)
	{
		if (cooldown <= 0)
		{
			throw new IllegalArgumentException("Cooldown must be positive.");
		}
		this.queue = new ConcurrentLinkedQueue<>();
		this.limit = itemLimit;
		this.cooldown = cooldown;
		this.onFlush = onFlush;
		this.itemCount = new AtomicInteger(0);
	}

	public void start()
	{
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::flush, cooldown, cooldown, TimeUnit.MILLISECONDS);
	}

	/**
	 * Flush, then shutdown the queues scheduled executor.
	 */
	public void shutdown()
	{
		this.flush();
		this.executor.shutdown();
	}

	public synchronized void flush()
	{
		if (itemCount.get() == 0)
		{
			return;
		}
		List<T> snapshot = new ArrayList<>();

		T item;
		while ((item = queue.poll()) != null)
		{
			snapshot.add(item);
		}

		itemCount.set(0);
		log.debug("[ironclad-clan-goals] Flushing batch queue.");

		onFlush.accept(snapshot);
	}

	/**
	 * Pushes an item to the queue.
	 * <p>
	 * Once the limit is hit then
	 * snapshot and flush items.
	 *
	 * @param item Item to push to the queue.
	 */
	public void addItem(T item)
	{
		if (item == null)
		{
			return;
		}
		queue.add(item);
		int curr = itemCount.incrementAndGet();

		if (limit > 0 && curr >= limit)
		{
			this.flush();
		}
	}
}
