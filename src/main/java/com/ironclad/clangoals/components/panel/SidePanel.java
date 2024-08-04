package com.ironclad.clangoals.components.panel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.ServiceComponent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

@Slf4j
@Singleton
class SidePanel extends PluginPanel
{
	private static final int SIZE = 16;
	public static final BufferedImage ICON = ImageUtil.loadImageResource(SidePanel.class, "icon.png");
	private static final BufferedImage ICON_INFO = ImageUtil.resizeImage(ImageUtil.loadImageResource(SidePanel.class, "info_circle.png"), SIZE, SIZE);
	private static final BufferedImage ICON_GOALS = ImageUtil.resizeImage(ImageUtil.loadImageResource(SidePanel.class, "flag.png"), SIZE, SIZE);

	private final String name;
	private final String version;
	private final HttpUrl apiBase;
	private final String github;
	private final String discord;
	private final ServiceComponent svc;

	@Inject
	public SidePanel(@Named("plugin.name") String name,
					 @Named("plugin.version.string") String version,
					 @Named("api.base") HttpUrl apiBase,
					 @Named("plugin.github") String github,
					 @Named("plugin.discord") String discord,
					 ServiceComponent svc)
	{
		super(false);
		this.name = name;
		this.version = version;
		this.apiBase = apiBase;
		this.github = github;
		this.discord = discord;
		this.svc = svc;
	}

	@Override
	public void onActivate()
	{
		init();
	}

	void init()
	{
		removeAll();
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());

		JPanel pnlLayout = new JPanel();
		pnlLayout.setLayout(new BoxLayout(pnlLayout, BoxLayout.Y_AXIS));

		pnlLayout.add(buildTitlePanel());
		pnlLayout.add(buildTabPanel());

		add(pnlLayout, BorderLayout.NORTH);
	}

	private JPanel buildTitlePanel()
	{
		JPanel pnlTitle = new JPanel();
		pnlTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		pnlTitle.setLayout(new BorderLayout());

		JLabel lblTitle = new JLabel(name);
		lblTitle.setForeground(ColorScheme.TEXT_COLOR);
		pnlTitle.add(lblTitle, BorderLayout.WEST);

		JLabel lblVersion = new JLabel(version);
		lblVersion.setForeground(ColorScheme.TEXT_COLOR);
		lblVersion.setFont(lblVersion.getFont().deriveFont(Font.BOLD, 12f));
		pnlTitle.add(lblVersion, BorderLayout.EAST);

		return pnlTitle;
	}

	private JPanel buildTabPanel()
	{
		JPanel pnlContainer = new JPanel();
		pnlContainer.setLayout(new BoxLayout(pnlContainer, BoxLayout.Y_AXIS));

		MaterialTabGroup tabGroup = new MaterialTabGroup();
		tabGroup.setLayout(new GridLayout(1, 3, 10, 10));
		tabGroup.setBorder(new EmptyBorder(0, 0, 10, 0));
		pnlContainer.add(tabGroup);

		JPanel pnlActive = new JPanel();
		pnlActive.setLayout(new BoxLayout(pnlActive, BoxLayout.Y_AXIS));
		pnlContainer.add(pnlActive);

		JPanel pnlInfo = buildInfoTab();
		pnlActive.add(pnlInfo);
		buildTab(new ImageIcon(ICON_INFO), "info", tabGroup, pnlInfo);
		JPanel pnlGoals = new JPanel();
		pnlActive.add(pnlGoals);
		buildTab(new ImageIcon(ICON_GOALS), "goals", tabGroup, pnlGoals).select();

		return pnlContainer;
	}

	private MaterialTab buildTab(ImageIcon icon, String tooltip, MaterialTabGroup group, JComponent content)
	{
		MaterialTab tab = new MaterialTab(icon, group, content);
		tab.setToolTipText(tooltip);
		group.addTab(tab);
		return tab;
	}

	private JPanel buildInfoTab()
	{
		JPanel pnlContainer = new JPanel();
		pnlContainer.setLayout(new BoxLayout(pnlContainer, BoxLayout.Y_AXIS));

		JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.setLayout(new GridLayout(0, 1));
		pnlContainer.add(panel);

		Font smol = FontManager.getRunescapeSmallFont();

		JLabel lblVersion = htmlLabel("Version: ", version);
		lblVersion.setFont(smol);
		JLabel lblAuthenticated = htmlLabel("Authenticated: ", svc.getState().isAuthenticated() ? "Yes" : "No");
		lblAuthenticated.setFont(smol);
		JLabel lblStatus = htmlLabel("Status: ", svc.getState().getRemoteConfig().isMaintenance() ? "Maintenance" : "Enabled");
		lblStatus.setFont(smol);
		JLabel lblEndpoint = htmlLabel("API: ", apiBase.toString());
		lblEndpoint.setFont(smol);

		panel.add(lblVersion);
		panel.add(lblAuthenticated);
		panel.add(lblStatus);
		panel.add(lblEndpoint);

		JPanel pnlButtons = new JPanel();
		pnlButtons.setLayout(new GridLayout(2, 1, 0, 5));
		pnlButtons.setBorder(new EmptyBorder(10, 0, 10, 0));
		pnlContainer.add(pnlButtons);

		JButton btnGithub = new JButton("Github");
		btnGithub.addActionListener(e -> LinkBrowser.browse(github));
		JButton btnDiscord = new JButton("Discord");
		btnDiscord.addActionListener(e -> LinkBrowser.browse(discord));

		pnlButtons.add(btnGithub);
		pnlButtons.add(btnDiscord);

		return pnlContainer;
	}

	private static JLabel htmlLabel(String key, String value)
	{
		return new JLabel("<html><body style = 'color:#a5a5a5'>" + key + "<span style = 'color:white'>" + value + "</span></body></html>");
	}
}
