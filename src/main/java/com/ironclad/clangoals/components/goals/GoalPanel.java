package com.ironclad.clangoals.components.goals;

import com.google.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
@Singleton
class GoalPanel extends PluginPanel
{
	public static final BufferedImage ICON = ImageUtil.loadImageResource(GoalPanel.class, "icon.png");

	public GoalPanel()
	{
		setBorder(BorderFactory.createEmptyBorder());
		setAlignmentX(CENTER_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		var btnRefresh = new JButton("Refresh");
		btnRefresh.setFocusable(false);
		btnRefresh.addActionListener(e -> log.info("Refresh button clicked"));

		add(btnRefresh, BorderLayout.NORTH);
	}
}
