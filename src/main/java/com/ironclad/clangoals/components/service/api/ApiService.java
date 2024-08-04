package com.ironclad.clangoals.components.service.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.util.Environment;
import com.ironclad.clangoals.util.IronClad;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;


@Slf4j
@Singleton
public class ApiService
{
	private static final MediaType JSON = MediaType.parse("application/json");
	private static final ValidApiKey VALID_API_KEY = new ValidApiKey();
	private static final long UNKNOWN = -1L;

	private final HttpUrl apiBase;
	private final String version;
	private final String characterEndpoint;
	private final String configEndpoint;
	private final String batchEndpoint;
	private final String goalEndpoint; //TODO implement
	private final boolean devServer;

	private final OkHttpClient httpClient;
	private final Client client;
	private final Gson gson;
	private String apiKey;
	@Getter
	private long accountHash = UNKNOWN;
	@Getter
	private boolean authenticated;

	@Inject
	public ApiService(@Named("api.base") HttpUrl apiBase,
					  @Named("plugin.version.string") String version,
					  @Named("api.endpoint.character") String characterEndpoint,
					  @Named("api.endpoint.config") String configEndpoint,
					  @Named("api.endpoint.batch") String batchEndpoint,
					  @Named("api.endpoint.goals") String goalEndpoint,
					  @Named("use.dev.server") boolean devServer,
					  OkHttpClient httpClient,
					  Client client,
					  @IronClad Gson gson)
	{
		this.apiBase = apiBase;
		this.version = version;
		this.characterEndpoint = characterEndpoint;
		this.configEndpoint = configEndpoint;
		this.batchEndpoint = batchEndpoint;
		this.goalEndpoint = goalEndpoint;
		this.devServer = devServer;
		this.httpClient = httpClient;
		this.client = client;
		this.gson = gson;
	}

	public void refreshAccountHash()
	{
		accountHash = client.getAccountHash();
		log.debug("Account hash updated to {}", accountHash);
	}

	/**
	 * Authenticate the API key against the server.
	 * <p>
	 * If an invalid value is passed, the auth will fail early, returning false.
	 * <p>
	 *
	 * @param apiKey API key to authenticate
	 * @return Future representing the result of the authentication
	 */
	public CompletableFuture<Boolean> authenticateAsync(@Nullable String apiKey)
	{
		if (devServer)
		{
			apiKey = Environment.DEV_KEY.get();
		}

		if (!VALID_API_KEY.test(apiKey) || Strings.isNullOrEmpty(apiKey))
		{
			authenticated = false;
			return CompletableFuture.completedFuture(false);
		}

		this.apiKey = apiKey;

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(characterEndpoint)
			.build();

		Request req = sharedRequest(url)
			.get()
			.build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();

		log.debug("Checking authentication...");

		httpClient.newCall(req)
			.enqueue(new Callback()
			{
				@Override
				public void onFailure(@NonNull Call call, @NonNull IOException e)
				{
					authenticated = false;
					future.completeExceptionally(e);
					log.debug("Authentication request failed");
				}

				@Override
				public void onResponse(@NonNull Call call, @NonNull Response response)
				{
					try (response)
					{
						authenticated = response.isSuccessful();
						future.complete(authenticated);
						log.debug("Authentication successful");
					}
					catch (Exception e)
					{
						future.completeExceptionally(e);
						log.debug("Error authenticating");
					}
				}
			});

		return future;
	}

	/**
	 * Fetch the plugin configuration from the server.
	 *
	 * @return CompletableFuture containing the RemoteConfiguration
	 */
	public CompletableFuture<RemoteConfig> getPluginConfigurationAsync()
	{
		if (!authenticated)
		{
			log.debug("Not authenticated, skipping get plugin configuration");
			return CompletableFuture.completedFuture(null);
		}

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(configEndpoint)
			.build();

		Request request = sharedRequest(url)
			.get()
			.build();

		log.debug("Getting remote configuration...");
		return sharedFuture(request, RemoteConfig.class);
	}

	@Nullable
	public RemoteConfig getPluginConfiguration()
	{
		if (!authenticated)
		{
			log.debug("Not authenticated, skipping get plugin configuration");
			return null;
		}

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(configEndpoint)
			.build();

		Request request = sharedRequest(url).get().build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				log.warn("Invalid response while getting plugin configuration: {}", response);
				return null;
			}

			return gson.fromJson(response.body().string(), RemoteConfig.class);
		}
		catch (IOException e)
		{
			log.warn("Error occurred getting plugin configuration ", e);
		}
		catch (JsonParseException ex)
		{
			log.warn("Json parsing error occurred getting plugin configuration ", ex);

		}
		return null;
	}


	/**
	 * Persist the account has with the current player name
	 * against the authenticated API key.
	 */
	public void updatePlayer(@NonNull String name)
	{
		if (!authenticated || accountHash == UNKNOWN || Strings.isNullOrEmpty(name))
		{
			log.debug("Not authenticated or account hash unknown, skipping update player");
			return;
		}

		JsonObject data = new JsonObject();

		data.addProperty("account_hash", accountHash);
		data.addProperty("character_name", name);

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(characterEndpoint)
			.build();

		Request req = sharedRequest(url)
			.put(body)
			.build();

		log.debug("Sending update character request for {}:{}", name, accountHash);

		httpClient.newCall(req).enqueue(sharedCallback(
			"character updated",
			"error updating character"
		));
	}

	/**
	 * Perform a batched update to the specified endpoint.
	 *
	 * @param endPoint  String representing the endpoint
	 * @param batch     List of {@link T}
	 * @param converter Function to convert {@link T} to {@link JsonObject}
	 * @param <T>       Type of the {@link T}
	 */
	public <T> void batchUpdate(@NonNull String endPoint, @NonNull List<T> batch, @NonNull Function<T, JsonObject> converter)
	{
		if (!authenticated || accountHash == UNKNOWN)
		{
			log.warn("Not authenticated or account hash unknown, skipping batch update");
			return;
		}

		JsonArray items = new JsonArray();

		for (T item : batch)
		{
			JsonObject o = converter.apply(item);
			if (o != null)
			{
				items.add(o);
			}
		}

		JsonObject data = new JsonObject();
		data.addProperty("account_hash", accountHash);
		data.add("batch", items);

		System.out.println(data.toString());//TODO REMOVE

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(batchEndpoint)
			.addPathSegment(endPoint)
			.build();

		Request request = sharedRequest(url)
			.put(body)
			.build();

		log.debug("Sending batch {} update", endPoint);

		httpClient.newCall(request).enqueue(sharedCallback(
			"Batch " + endPoint + " updated",
			"error updating batch " + endPoint
		));
	}

	/**
	 * Shared request headers for all requests.
	 */
	private Request.Builder sharedRequest(HttpUrl url)
	{
		return new Request
			.Builder()
			.url(url)
			.header("accept", "application/json")
			.header("content-type", "application/json")
			.header("authorization", apiKey)
			.header("x-account-hash", String.valueOf(accountHash))
			.header("x-plugin-version", version);
	}


	private Callback sharedCallback(Consumer<Response> responseConsumer, Consumer<Exception> exceptionConsumer)
	{
		return new Callback()
		{
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e)
			{
				if (exceptionConsumer == null)
				{
					log.warn("Error sending request", e);
				}
				else
				{
					exceptionConsumer.accept(e);
				}
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response)
			{
				try (response)
				{
					responseConsumer.accept(response);
				}
				catch (Exception e)
				{
					onFailure(call, new IOException(e));
				}
			}
		};
	}

	/**
	 * Shared client callback.
	 */
	private Callback sharedCallback(String successMessage, String errorMessage)
	{
		return sharedCallback(
			response -> {
				try (response)
				{
					if (response.isSuccessful())
					{
						log.debug("Success: " + successMessage);
					}
					else
					{
						log.warn("Failed: {} Code: {} Message: {}", errorMessage, response.code(), response.message());
					}
				}
			},
			e -> log.warn("Error: {}", errorMessage, e)
		);
	}

	/**
	 * Shared future for all get requests.
	 *
	 * @param request OkHttp request
	 * @param resType TypeToken representing the type T.
	 * @param <T>     Type of the object the response will be parsed into
	 * @return CompletableFuture
	 */
	private <T> CompletableFuture<T> sharedFuture(Request request, Class<T> resType)
	{
		CompletableFuture<T> future = new CompletableFuture<>();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e)
			{
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response)
			{
				try (response)
				{
					if (!response.isSuccessful())
					{
						log.warn("Request to {} failed with code: {}", request.url(), response.code());
						throw new IOException("Request failed with code: " + response.code());
					}
					if (response.body() != null)
					{
						String body = response.body().string();
						T result = gson.fromJson(body, resType);
						if (result == null)
						{
							log.warn("Failed to parse response body: {}", body);
							throw new IOException("Failed to parse response body: " + body);
						}
						future.complete(result);
					}
				}
				catch (Exception e)
				{
					future.completeExceptionally(e);
				}
			}
		});
		return future;
	}
}
