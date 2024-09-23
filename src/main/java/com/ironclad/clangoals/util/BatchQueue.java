package com.ironclad.clangoals.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

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
		if (this.isShutdown.compareAndSet(true, false))
		{
			this.executor = executor;
			this.scheduledFuture = executor.scheduleWithFixedDelay(this::runFlush, this.interval, this.interval, TimeUnit.SECONDS);
		}
	}

	/**
	 * Flush, then shutdown the queues scheduled executor.
	 */
	public void shutdown()
	{
		if (this.isShutdown.compareAndSet(false, true))
		{
			log.debug("Shutting down BatchQueue");
			flush();
			this.scheduledFuture.cancel(true);
		}
	}

	public void flush()
	{
		if(this.isShutdown.get())
		{
			return;
		}
		this.executor.execute(this::runFlush);
	}

	/**
	 * Force a flush of the queue.
	 */
	private void runFlush()
	{
		if (this.itemCount.get() == 0)
		{
			return;
		}

		List<T> snapshot = new ArrayList<>();

		T item;
		while ((item = this.queue.poll()) != null)
		{
			snapshot.add(item);
		}

		this.itemCount.set(0);

		try
		{
			this.onFlush.accept(snapshot);
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
		if (item == null || this.isShutdown.get())
		{
			return;
		}

		this.queue.add(item);

		int curr = this.itemCount.incrementAndGet();

		if (this.limit > 0 && curr >= this.limit)
		{
			this.flush();
		}
	}
}
