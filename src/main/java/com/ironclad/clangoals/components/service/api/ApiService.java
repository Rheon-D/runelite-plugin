package com.ironclad.clangoals.components.service.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ironclad.clangoals.components.service.config.dto.RemoteConfig;
import com.ironclad.clangoals.util.Environment;
import com.ironclad.clangoals.util.IronClad;
import com.ironclad.clangoals.util.predicate.ValidApiKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
	private final Gson gson;
	private String apiKey;
	@Getter
	@Setter
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
					  @Named("devServer") boolean devServer,
					  OkHttpClient httpClient,
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
		this.gson = gson;
	}

	/**
	 * Authenticate the API key against the server.
	 * <p>
	 * If an invalid value is passed, the auth will fail early, returning false.
	 * <p>
	 *
	 * @param apiKey API key to authenticate
	 * @return Result of authentication
	 */
	public boolean checkAuth(@Nullable String apiKey)
	{
		if (!VALID_API_KEY.test(apiKey))
		{
			this.authenticated = false;
			return false;
		}

		if (this.devServer)
		{
			apiKey = Environment.DEV_KEY.get();
		}

		this.apiKey = apiKey;

		HttpUrl url = this.apiBase.newBuilder()
			.addPathSegment(this.characterEndpoint)
			.build();

		Request req = new Request.Builder()
			.url(url)
			.header("accept", "application/json")
			.header("content-type", "application/json")
			.header("authorization", apiKey)
			.header("x-plugin-version", this.version)
			.get()
			.build();

		log.debug("Checking authentication...");

		try (Response res = this.httpClient.newCall(req).execute())
		{
			this.authenticated = res.isSuccessful();
			log.debug("Authentication successful");
		}
		catch (Exception e)
		{
			this.authenticated = false;
			log.debug("Authentication request failed");
		}

		return this.authenticated;
	}

	@NonNull
	public CompletableFuture<RemoteConfig> getPluginConfiguration(@NonNull String apiKey)
	{
		CompletableFuture<RemoteConfig> future = new CompletableFuture<>();
		HttpUrl url = this.apiBase.newBuilder()
			.addPathSegment(this.configEndpoint)
			.build();

		Request request = new Request.Builder()
			.url(url)
			.header("accept", "application/json")
			.header("content-type", "application/json")
			.header("authorization", apiKey)
			.header("x-plugin-version", this.version)
			.get()
			.build();

		this.httpClient.newCall(request).enqueue(new Callback()
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
					if (!response.isSuccessful() || response.body() == null)
					{
						throw new IOException("Invalid response while getting plugin configuration: " + response);
					}

					String body = response.body().string();
					RemoteConfig result = ApiService.this.gson.fromJson(body, RemoteConfig.class);
					if (result == null)
					{
						throw new IOException("Failed to parse response body: " + body);
					}
					log.debug("Got plugin configuration: {}", result);
					future.complete(result);

				}
				catch (Exception e)
				{
					log.warn(e.getMessage());
					future.completeExceptionally(e);
				}
			}
		});
		return future;
	}

	/**
	 * Persist the account has with the current player name
	 * against the authenticated API key.
	 */
	public void updatePlayerAsync(@NonNull String name)
	{
		if (!this.authenticated || this.accountHash == UNKNOWN || Strings.isNullOrEmpty(name))
		{
			log.debug("Not authenticated or account hash unknown, skipping update player");
			return;
		}

		JsonObject data = new JsonObject();

		data.addProperty("account_hash", this.accountHash);
		data.addProperty("character_name", name);

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = this.apiBase.newBuilder()
			.addPathSegment(this.characterEndpoint)
			.build();

		Request req = sharedRequest(url)
			.put(body)
			.build();

		log.debug("Sending update character request for {}:{}", name, this.accountHash);

		this.httpClient.newCall(req).enqueue(sharedCallback(
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
	public <T> void batchUpdateAsync(@NonNull String endPoint, @NonNull List<T> batch, @NonNull Function<T, JsonObject> converter)
	{
		if (!this.authenticated || this.accountHash == UNKNOWN)
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
		data.addProperty("account_hash", this.accountHash);
		data.add("batch", items);

		RequestBody body = RequestBody.create(JSON, data.toString());

		HttpUrl url = this.apiBase.newBuilder()
			.addPathSegment(this.batchEndpoint)
			.addPathSegment(endPoint)
			.build();

		Request request = sharedRequest(url)
			.put(body)
			.build();

		log.debug("Sending batch {} update", endPoint);

		this.httpClient.newCall(request).enqueue(sharedCallback(
			"Batch " + endPoint + " updated",
			"error updating batch " + endPoint
		));
	}

	/**
	 * Request headers for all account based requests.
	 */
	private Request.Builder sharedRequest(HttpUrl url)
	{
		Preconditions.checkArgument(this.accountHash != UNKNOWN || this.accountHash == 0, "Account hash must be set before making requests");
		return new Request
			.Builder()
			.url(url)
			.header("accept", "application/json")
			.header("content-type", "application/json")
			.header("authorization", this.apiKey)
			.header("x-account-hash", String.valueOf(this.accountHash))
			.header("x-plugin-version", this.version);
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
		this.httpClient.newCall(request).enqueue(new Callback()
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
						T result = ApiService.this.gson.fromJson(body, resType);
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
