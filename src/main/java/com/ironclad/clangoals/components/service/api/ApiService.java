package com.ironclad.clangoals.components.service.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.dto.RemoteConfig;
import com.ironclad.clangoals.components.service.dto.XPGoal;
import com.ironclad.clangoals.util.Environment;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static com.ironclad.clangoals.components.service.api.Endpoint.*;

@Slf4j
public final class ApiService
{
	private static final MediaType JSON = MediaType.parse("application/json");
	private static final long UNKNOWN = -1L;

	private final HttpUrl apiBase;
	private final boolean devServer;
	private final String version;
	private final OkHttpClient httpClient;
	private final Gson gson;
	private String apiKey;
	@Getter
	private long accountHash = UNKNOWN;
	@Getter
	private boolean authenticated;

	@Inject
	public ApiService(@Named("api.base") HttpUrl apiBase, @Named("use.dev.server") boolean devServer,
					  @Named("version.string") String version, OkHttpClient httpClient, Gson gson)
	{
		this.apiBase = apiBase;
		this.devServer = devServer;
		this.version = version;
		this.httpClient = httpClient;
		this.gson = gson;
	}


	/**
	 * Authenticate the API key against the server.
	 *
	 * @param apiKey API key to authenticate
	 * @return Future representing the result of the authentication
	 */
	public CompletableFuture<Boolean> authenticate(String apiKey)
	{
		if (devServer)
		{
			apiKey = Environment.DEV_KEY.get();
		}

		if (Strings.isNullOrEmpty(apiKey))
		{
			authenticated = false;
			return CompletableFuture.completedFuture(false);
		}

		this.apiKey = apiKey;

		var url = apiBase.newBuilder()
			.addPathSegment(ME.getPath())
			.build();

		var req = sharedRequest(url)
			.get()
			.build();

		var future = new CompletableFuture<Boolean>();

		httpClient.newCall(req)
			.enqueue(new Callback()
			{
				@Override
				public void onFailure(@NonNull Call call, @NonNull IOException e)
				{
					authenticated = false;
					future.completeExceptionally(e);
				}

				@Override
				public void onResponse(@NonNull Call call, @NonNull Response response)
				{
					try (response)
					{
						authenticated = response.isSuccessful();
						future.complete(authenticated);
					}
					catch (Exception e)
					{
						future.completeExceptionally(e);
					}
				}
			});

		return future;
	}

	/**
	 * Fetch the plugin configuration from the server.
	 *
	 * @return Future representing the result of the request
	 */
	public CompletableFuture<RemoteConfig> getPluginConfiguration()
	{
		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(CONFIGURATION.getPath())
			.build();

		Request request = sharedRequest(url)
			.get()
			.build();

		return sharedFuture(request, new TypeToken<RemoteConfig>()
		{
		});
	}

	/**
	 * Persist the account has with the current player name
	 * against the authenticated API key.
	 */
	public void updatePlayer(@Nullable Player player)
	{
		if (player == null)
		{
			accountHash = UNKNOWN;
			return;
		}

		JsonObject data = new JsonObject();
		accountHash = player.getHash();

		data.addProperty("account_hash", accountHash);
		data.addProperty("character_name", player.getName());

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(CHARACTERS.getPath())
			.build();

		Request req = sharedRequest(url)
			.put(body)
			.build();

		log.debug("[ironclad-clan-goals] send update character request");

		httpClient.newCall(req).enqueue(sharedCallback(
			"character updated",
			"error updating character"
		));
	}

	/**
	 * Perform a batched update to the specified endpoint.
	 *
	 * @param endPoint  {@link Endpoint}
	 * @param batch     List of {@link T}
	 * @param converter Function to convert {@link T} to {@link JsonObject}
	 * @param <T>       Type of the {@link T}
	 */
	public <T> void batchUpdate(Endpoint endPoint, List<T> batch, Function<T, JsonObject> converter)
	{
		if (!authenticated || accountHash == UNKNOWN)
		{
			log.warn("[ironclad-clan-goals] Not authenticated or account hash unknown, skipping batch update");
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

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(BATCH.getPath())
			.addPathSegment(endPoint.getPath())
			.build();

		Request request = sharedRequest(url)
			.put(body)
			.build();

		httpClient.newCall(request).enqueue(sharedCallback(
			"Batch " + endPoint.getPath() + " updated",
			"error updating batch " + endPoint.getPath()
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

	/**
	 * Shared client callback.
	 */
	private Callback sharedCallback(String successMessage, String errorMessage)
	{
		return new Callback()
		{
			@Override
			public void onFailure(
				@NonNull
				Call call,
				@NonNull
				IOException e
			)
			{
				log.warn("Error: " + errorMessage);
			}

			@Override
			public void onResponse(
				@NonNull
				Call call,
				@NonNull
				Response response
			)
			{
				if (response.isSuccessful())
				{
					log.debug("Success: " + successMessage);
				}
				else
				{
					log.warn("Failed:  {} Code:{} Message:{}", errorMessage, response.code(), response.message());
				}
				response.close();
			}
		};
	}

	public CompletableFuture<List<XPGoal>> getGoals()
	{
		HttpUrl url = apiBase.newBuilder()
			.addPathSegment(GOALS.getPath())
			.build();

		Request request = sharedRequest(url)
			.get()
			.build();

		return sharedFuture(request, new TypeToken<List<XPGoal>>()
		{
		});
	}


	/**
	 * Shared future for all get requests.
	 *
	 * @param request OkHttp request
	 * @param resType TypeToken representing the type T.
	 * @param <T>     Type of the object the response will be parsed into
	 * @return CompletableFuture
	 */
	private <T> CompletableFuture<T> sharedFuture(Request request, TypeToken<T> resType)
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
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
			{
				try (response)
				{
					if (!response.isSuccessful())
					{
						throw new IOException("Request failed with code: " + response.code());
					}
					if (response.body() != null)
					{
						T result = gson.fromJson(response.body().string(), resType.getType());
						if (result == null)
						{
							throw new IOException("Failed to parse response body");
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
