package com.ironclad.clangoals;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ironclad.clangoals.components.status.StatusComponent;
import com.ironclad.clangoals.components.service.ServiceComponent;
import com.ironclad.clangoals.components.service.config.RemoteConfigRefresher;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingComponent;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingComponent;
import com.ironclad.clangoals.components.service.config.RemoteConfigLoader;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingDevOverlay;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingComponent;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.util.Environment;
import com.ironclad.clangoals.util.IronClad;
import com.ironclad.clangoals.util.gson.PatternAdapter;
import java.util.regex.Pattern;
import net.runelite.client.config.ConfigManager;
import okhttp3.HttpUrl;

final class IroncladModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		Names.bindProperties(binder(), PluginProperties.getProperties());

		binder().bindConstant().annotatedWith(Names.named("devServer")).to(Environment.DEV_KEY.isSet() && Environment.DEV_URL.isSet());

		bind(ApiService.class).asEagerSingleton();
		bind(RemoteConfigLoader.class).asEagerSingleton();

		Multibinder<Component> components = Multibinder.newSetBinder(binder(), Component.class);
		components.addBinding().to(ServiceComponent.class);
		components.addBinding().to(StatusComponent.class);
		components.addBinding().to(XpTrackingComponent.class);
		components.addBinding().to(ItemTrackingComponent.class);
		components.addBinding().to(NPCTrackingComponent.class);
		components.addBinding().to(NPCTrackingDevOverlay.class);
		components.addBinding().to(RemoteConfigRefresher.class);
	}

	@Provides
	@Singleton
	@IronClad
	Gson provideGson(Gson gson)
	{
		//All gson instances tagged with our anno should have custom type adapters.
		//Why? because reasons. Could have used a @Named, but this is more fun.
		return gson.newBuilder()
			.registerTypeAdapter(Pattern.class, new PatternAdapter())
			.create();
	}

	@Provides
	@Singleton
	@Named("api.base")
	HttpUrl provideApiBase(@Named("api.base") String url, @Named("devServer") boolean useEnv, @Named("developerMode") boolean devMode)
	{
		return HttpUrl.parse(useEnv && devMode ? Environment.DEV_URL.get() : url);
	}

	@Provides
	@Singleton
	RemoteConfig provideRemoteConfig(RemoteConfigLoader configLoader)
	{
		return configLoader.getManagedConfig();
	}

	@Provides
	@Singleton
	IroncladClanGoalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(IroncladClanGoalsConfig.class);
	}
}
