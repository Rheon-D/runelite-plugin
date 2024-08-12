package com.ironclad.clangoals.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ExecutorServiceExceptionLogger;

@Slf4j
public final class BatchQueue<T>
{
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> scheduledFuture;
	private final ConcurrentLinkedQueue<T> queue;
	private final int limit;
	private final AtomicInteger itemCount;
	/**
	 * MS between flush attempts.
	 */
	private final long interval;
	private final Consumer<List<T>> onFlush;

	private final AtomicBoolean isShutdown = new AtomicBoolean(true);

	/**
	 * @param itemLimit Flush when items reach this amount. -1 to disable.
	 * @param interval  S, The interval between flush attempts.
	 */
	public BatchQueue(int itemLimit, long interval, Consumer<List<T>> onFlush)
	{
		if (interval <= 0)
		{
			throw new IllegalArgumentException("Interval must be positive.");
		}
		this.queue = new ConcurrentLinkedQueue<>();
		this.limit = itemLimit;
		this.interval = interval;
		this.onFlush = onFlush;
		this.itemCount = new AtomicInteger(0);
	}

	public void start(ScheduledExecutorService executor)
	{
		log.debug("Starting BatchQueue");
		if (isShutdown.compareAndSet(true, false))
		{
			this.executor = executor;
			this.scheduledFuture = executor.scheduleWithFixedDelay(this::runFlush, interval, interval, TimeUnit.SECONDS);
		}
	}

	/**
	 * Flush, then shutdown the queues scheduled executor.
	 */
	public void shutdown()
	{
		log.debug("Shutting down BatchQueue");
		if (isShutdown.compareAndSet(false, true))
		{
			flush();
			scheduledFuture.cancel(true);
		}
	}

	public void flush()
	{
		if(isShutdown.get())
		{
			return;
		}
		executor.execute(this::runFlush);
	}

	/**
	 * Force a flush of the queue.
	 */
	private void runFlush()
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

		try
		{
			onFlush.accept(snapshot);
		}
		catch (Exception e)
		{
			log.error("Error during flush", e);
		}
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
		if (item == null || isShutdown.get())
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
