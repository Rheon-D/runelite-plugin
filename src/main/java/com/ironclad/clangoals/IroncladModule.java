package com.ironclad.clangoals;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ironclad.clangoals.components.service.ServiceComponent;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingComponent;
import com.ironclad.clangoals.components.tracking.loot.ItemTrackingComponent;
import com.ironclad.clangoals.components.service.config.ConfigService;
import com.ironclad.clangoals.components.tracking.npcs.NPCTrackingDevOverlay;
import com.ironclad.clangoals.components.tracking.xp.XpTrackingComponent;
import com.ironclad.clangoals.components.service.api.ApiService;
import com.ironclad.clangoals.component.Component;
import com.ironclad.clangoals.util.Environment;
import com.ironclad.clangoals.util.IronClad;
import java.time.Instant;
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
		components.addBinding().to(XpTrackingComponent.class);
		components.addBinding().to(ItemTrackingComponent.class);
		components.addBinding().to(NPCTrackingComponent.class);
		components.addBinding().to(NPCTrackingDevOverlay.class);
	}

	@Provides
	@IronClad
	Gson provideGson(Gson gson)
	{
		//All gson instances tagged with our anno should have custom type adapters.
		return gson.newBuilder()
			.registerTypeAdapter(Instant.class,
				(JsonDeserializer<Instant>) (jsonElement, type, jsonDeserializationContext) ->
					Instant.parse(jsonElement.getAsString()))
			.create();
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
