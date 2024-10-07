package com.ironclad.clangoals.components.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ironclad.clangoals.IroncladClanGoalsConfig;
import com.ironclad.clangoals.IroncladClanGoalsPlugin;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.components.service.PluginState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@Singleton
public class EventOverlayComponent extends OverlayPanel implements Component
{
	private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyy HH:mm z").withZone(TimeZone.getTimeZone("UTC").toZoneId());
	private final IroncladClanGoalsConfig pluginConfig;
	private final OverlayManager overlayManager;
	private final EventBus eventBus;
	private final KeyManager keymanager;
	private final HotkeyListener overlayHotkey;
	boolean onScreen = false;

	@Inject
	public EventOverlayComponent(IroncladClanGoalsPlugin plugin,
								 IroncladClanGoalsConfig pluginConfig,
								 OverlayManager overlayManager,
								 EventBus eventBus,
								 KeyManager keyManager)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_RIGHT);
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Event Password"));

		this.pluginConfig = pluginConfig;
		this.overlayManager = overlayManager;
		this.eventBus = eventBus;
		this.keymanager = keyManager;
		this.overlayHotkey = new HotkeyListener(this.pluginConfig::eventOverlayKeybind)
		{
			@Override
			public void hotkeyPressed()
			{
				toggleOverlay();
			}
		};
	}

	@Override
	public void onStartUp(PluginState state)
	{
		this.eventBus.register(this);
		this.keymanager.registerKeyListener(this.overlayHotkey);
		toggleOverlay();
	}

	@Override
	public void onShutDown(PluginState state)
	{
		this.overlayManager.remove(this);
		this.eventBus.unregister(this);
		this.keymanager.unregisterKeyListener(this.overlayHotkey);
		this.onScreen = false;
	}

	@Override
	public boolean isEnabled(IroncladClanGoalsConfig config, PluginState state)
	{
		return config.eventOverlay();
	}

	private void addOverlay()
	{
		this.overlayManager.add(this);
		this.onScreen = true;
	}

	private void removeOverlay()
	{
		this.overlayManager.remove(this);
		this.onScreen = false;
	}

	private void toggleOverlay()
	{
		if (this.onScreen)
		{
			removeOverlay();
		}
		else
		{
			addOverlay();
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged e)
	{
		if (!e.getGroup().equalsIgnoreCase(IroncladClanGoalsConfig.CONFIG_GROUP))
		{
			return;
		}

		if (e.getKey().equalsIgnoreCase(IroncladClanGoalsConfig.EVENT_OVERLAY_KEYBIND))
		{
			if (this.pluginConfig.eventOverlayKeybind().equals(Keybind.NOT_SET))
			{
				this.addOverlay();
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String password = this.pluginConfig.eventPassword();

		if (Strings.isNullOrEmpty(password))
		{
			return null;
		}

		String timestamp = UTC_FORMATTER.format(Instant.now());
		String allText = password + " " + timestamp;

		this.panelComponent.getChildren().add(LineComponent.builder()
			.leftColor(this.pluginConfig.eventPasswordColor())
			.left(password)
			.rightColor(this.pluginConfig.eventTimestampColor())
			.right(timestamp)
			.build());

		this.panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(allText) + 10, 0));

		return super.render(graphics);
	}
}
