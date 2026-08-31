package net.dyhntastic.radio.core.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.dyhntastic.radio.core.RadioConstants;

public final class HttpJsonClient implements AutoCloseable {

  private final HttpClient client;

  public HttpJsonClient(Executor executor) {
    this.client = HttpClient.newBuilder()
        .executor(executor)
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public CompletableFuture<JsonElement> get(String url) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Accept", "application/json")
        .header("User-Agent", RadioConstants.USER_AGENT)
        .GET()
        .build();
    return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " for " + url);
          }
          return JsonParser.parseString(response.body());
        });
  }

  @Override
  public void close() {
    // java.net.http.HttpClient owns no closeable resources on Java 21.
  }
}
