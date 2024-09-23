package com.ironclad.clangoals.components.tracking;

import com.ironclad.clangoals.components.service.config.Updatable;
import com.ironclad.clangoals.components.service.config.dto.QueueConfig;

public interface TrackingConfig<T> extends Updatable<T>
{
	boolean isEnabled();
	QueueConfig getQueueConfig();
}
