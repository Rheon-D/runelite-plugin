package com.ironclad.clangoals;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ironclad.clangoals.components.goals.GoalComponent;
import com.ironclad.clangoals.components.service.ServiceComponent;
import com.ironclad.clangoals.components.tracking.NpcTrackingComponent;
import com.ironclad.clangoals.components.tracking.ItemTrackingComponent;
import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.tracking.XpTrackingComponent;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.util.Environment;
import net.runelite.client.config.ConfigManager;
import okhttp3.HttpUrl;

final class IroncladModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		Names.bindProperties(binder(), PluginProperties.getProperties());
		binder().bindConstant().annotatedWith(Names.named("use.dev.server")).to(Environment.DEV_KEY.isSet() && Environment.DEV_URL.isSet());

		bind(ApiService.class).asEagerSingleton();
		bind(ConfigService.class).asEagerSingleton();

		Multibinder<Component> components = Multibinder.newSetBinder(binder(), Component.class);
		components.addBinding().to(ServiceComponent.class);
		components.addBinding().to(GoalComponent.class);
		components.addBinding().to(XpTrackingComponent.class);
		components.addBinding().to(ItemTrackingComponent.class);
		components.addBinding().to(NpcTrackingComponent.class);
	}

	@Provides
	@Named("api.base")
	HttpUrl provideApiBase(@Named("api.base") String url, @Named("use.dev.server")boolean useEnv, @Named("developerMode") boolean devMode)
	{
		return HttpUrl.parse(useEnv && devMode? Environment.DEV_URL.get() : url);
	}

	@Provides
	@Singleton
	IroncladClanGoalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(IroncladClanGoalsConfig.class);
	}
}
