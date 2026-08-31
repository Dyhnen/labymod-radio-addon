package net.dyhntastic.radio.core.presence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.PresencePrivacy;
import net.dyhntastic.radio.core.RadioConfiguration;
import net.dyhntastic.radio.core.util.UrlValidator;
import net.labymod.api.LabyAPI;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.labymod.labyconnect.session.LabyConnectBroadcastEvent;
import net.labymod.api.labyconnect.LabyConnect;
import net.labymod.api.labyconnect.LabyConnectSession;

/** Synchronizes the small now-playing name-tag card through LabyConnect. */
public final class RadioPresenceService implements AutoCloseable {

  static final String BROADCAST_KEY = "dyhnunity-radio:now-playing-v1";
  static final int PAYLOAD_VERSION = 1;
  static final long HEARTBEAT_MILLIS = 15_000L;
  static final long REMOTE_TIMEOUT_MILLIS = 35_000L;
  private static final int MAX_STATION_LENGTH = 128;
  private static final int MAX_TITLE_LENGTH = 256;
  private static final int MAX_ARTIST_LENGTH = 192;
  private static final int MAX_ARTWORK_LENGTH = 2_048;

  private final LabyAPI labyAPI;
  private final ConfigProperty<Boolean> enabled;
  private final ConfigProperty<PresencePrivacy> privacy;
  private final LongSupplier clock;
  private final Map<UUID, RemoteRadioPresence> remotePresences = new ConcurrentHashMap<>();
  private volatile String lastFingerprint = "";
  private volatile PresencePrivacy lastPrivacy = PresencePrivacy.NOBODY;
  private volatile long lastBroadcastAt;
  private volatile boolean activeBroadcast;
  private volatile boolean closed;

  public RadioPresenceService(LabyAPI labyAPI, RadioConfiguration configuration) {
    this(
        labyAPI,
        configuration.presenceEnabled(),
        configuration.presencePrivacy(),
        System::currentTimeMillis
    );
  }

  RadioPresenceService(
      LabyAPI labyAPI,
      ConfigProperty<Boolean> enabled,
      ConfigProperty<PresencePrivacy> privacy,
      LongSupplier clock
  ) {
    this.labyAPI = labyAPI;
    this.enabled = enabled;
    this.privacy = privacy;
    this.clock = clock;
  }

  /** Called periodically so state changes and metadata updates arrive without UI interaction. */
  public void synchronize(RadioStation station, PlayerState state) {
    if (this.closed) {
      return;
    }
    long now = this.clock.getAsLong();
    this.removeExpired(now);

    PresencePrivacy selectedPrivacy = this.selectedPrivacy();
    boolean sharingEnabled = Boolean.TRUE.equals(this.enabled.get())
        && selectedPrivacy != PresencePrivacy.NOBODY;
    if (!sharingEnabled) {
      this.remotePresences.clear();
      this.clearActiveBroadcast();
      return;
    }
    if (station == null || !isVisibleState(state)) {
      this.clearActiveBroadcast();
      return;
    }

    JsonObject payload = createPayload(station, state);
    String fingerprint = payload.toString();
    boolean privacyChanged = this.activeBroadcast && selectedPrivacy != this.lastPrivacy;
    if (privacyChanged) {
      this.send(this.lastPrivacy, clearPayload());
      this.activeBroadcast = false;
      this.lastFingerprint = "";
    }

    if (!fingerprint.equals(this.lastFingerprint)
        || !this.activeBroadcast
        || now - this.lastBroadcastAt >= HEARTBEAT_MILLIS) {
      if (this.send(selectedPrivacy, payload)) {
        this.lastFingerprint = fingerprint;
        this.lastPrivacy = selectedPrivacy;
        this.lastBroadcastAt = now;
        this.activeBroadcast = true;
      }
    }
  }

  public RemoteRadioPresence remotePresence(UUID playerId) {
    PresencePrivacy selectedPrivacy = this.selectedPrivacy();
    if (playerId == null
        || !Boolean.TRUE.equals(this.enabled.get())
        || selectedPrivacy == PresencePrivacy.NOBODY) {
      return null;
    }
    if (selectedPrivacy == PresencePrivacy.FRIENDS && !this.isFriend(playerId)) {
      this.remotePresences.remove(playerId);
      return null;
    }
    RemoteRadioPresence presence = this.remotePresences.get(playerId);
    if (presence != null
        && this.clock.getAsLong() - presence.receivedAtMillis() > REMOTE_TIMEOUT_MILLIS) {
      this.remotePresences.remove(playerId, presence);
      return null;
    }
    return presence;
  }

  @Subscribe
  public void onBroadcast(LabyConnectBroadcastEvent event) {
    if (this.closed
        || event.action() != LabyConnectBroadcastEvent.Action.RECEIVE
        || !BROADCAST_KEY.equals(event.getKey())
        || event.getSender() == null) {
      return;
    }

    UUID sender = event.getSender();
    PresencePrivacy selectedPrivacy = this.selectedPrivacy();
    if (!Boolean.TRUE.equals(this.enabled.get())
        || selectedPrivacy == PresencePrivacy.NOBODY
        || selectedPrivacy == PresencePrivacy.FRIENDS && !this.isFriend(sender)) {
      this.remotePresences.remove(sender);
      return;
    }

    RemoteRadioPresence decoded = decode(sender, event.getPayload(), this.clock.getAsLong());
    if (decoded == null) {
      this.remotePresences.remove(sender);
    } else {
      this.remotePresences.put(sender, decoded);
    }
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    this.clearActiveBroadcast();
    this.remotePresences.clear();
    this.closed = true;
  }

  static JsonObject createPayload(RadioStation station, PlayerState state) {
    RadioMetadata metadata = station.metadata() == null ? RadioMetadata.EMPTY : station.metadata();
    String artwork = metadata.coverUrl().isBlank() ? station.logoUrl() : metadata.coverUrl();
    JsonObject payload = new JsonObject();
    payload.addProperty("v", PAYLOAD_VERSION);
    payload.addProperty("active", true);
    payload.addProperty("state", state.name());
    payload.addProperty("station", sanitize(station.name(), MAX_STATION_LENGTH));
    payload.addProperty("title", sanitize(metadata.title(), MAX_TITLE_LENGTH));
    payload.addProperty("artist", sanitize(metadata.artist(), MAX_ARTIST_LENGTH));
    payload.addProperty("artwork", sanitizeArtwork(artwork));
    return payload;
  }

  static JsonObject clearPayload() {
    JsonObject payload = new JsonObject();
    payload.addProperty("v", PAYLOAD_VERSION);
    payload.addProperty("active", false);
    return payload;
  }

  static RemoteRadioPresence decode(UUID sender, JsonElement element, long receivedAtMillis) {
    if (sender == null || element == null || !element.isJsonObject()) {
      return null;
    }
    try {
      JsonObject payload = element.getAsJsonObject();
      if (!payload.has("v")
          || payload.get("v").getAsInt() != PAYLOAD_VERSION
          || !payload.has("active")
          || !payload.get("active").getAsBoolean()) {
        return null;
      }
      PlayerState state = PlayerState.valueOf(readString(payload, "state", 32));
      if (!isVisibleState(state)) {
        return null;
      }
      String station = readString(payload, "station", MAX_STATION_LENGTH);
      if (station.isBlank()) {
        return null;
      }
      return new RemoteRadioPresence(
          sender,
          station,
          readString(payload, "title", MAX_TITLE_LENGTH),
          readString(payload, "artist", MAX_ARTIST_LENGTH),
          sanitizeArtwork(readString(payload, "artwork", MAX_ARTWORK_LENGTH)),
          state,
          receivedAtMillis
      );
    } catch (IllegalArgumentException | IllegalStateException exception) {
      return null;
    }
  }

  private void clearActiveBroadcast() {
    if (this.activeBroadcast && this.send(this.lastPrivacy, clearPayload())) {
      this.activeBroadcast = false;
      this.lastFingerprint = "";
      this.lastBroadcastAt = 0L;
      this.lastPrivacy = PresencePrivacy.NOBODY;
    }
  }

  private boolean send(PresencePrivacy target, JsonObject payload) {
    if (target == null || target == PresencePrivacy.NOBODY || this.labyAPI == null) {
      return false;
    }
    try {
      LabyConnect labyConnect = this.labyAPI.labyConnect();
      if (labyConnect == null
          || !labyConnect.isAuthenticated()
          || !labyConnect.isConnectionEstablished()) {
        return false;
      }
      LabyConnectSession session = labyConnect.getSession();
      if (session == null) {
        return false;
      }
      if (target == PresencePrivacy.FRIENDS) {
        session.sendBroadcastPayload(BROADCAST_KEY, payload);
      } else {
        session.sendSurroundingBroadcastPayload(BROADCAST_KEY, payload);
      }
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private boolean isFriend(UUID playerId) {
    if (this.labyAPI == null) {
      return false;
    }
    try {
      LabyConnect labyConnect = this.labyAPI.labyConnect();
      LabyConnectSession session = labyConnect == null ? null : labyConnect.getSession();
      return session != null && session.getFriend(playerId) != null;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private PresencePrivacy selectedPrivacy() {
    PresencePrivacy selected = this.privacy.get();
    return selected == null ? PresencePrivacy.NOBODY : selected;
  }

  private void removeExpired(long now) {
    this.remotePresences.entrySet().removeIf(
        entry -> now - entry.getValue().receivedAtMillis() > REMOTE_TIMEOUT_MILLIS
    );
  }

  private static boolean isVisibleState(PlayerState state) {
    return state == PlayerState.LOADING
        || state == PlayerState.PLAYING
        || state == PlayerState.PAUSED;
  }

  private static String readString(JsonObject payload, String name, int maxLength) {
    if (!payload.has(name) || payload.get(name).isJsonNull()) {
      return "";
    }
    return sanitize(payload.get(name).getAsString(), maxLength);
  }

  private static String sanitizeArtwork(String value) {
    String artwork = sanitize(value, MAX_ARTWORK_LENGTH);
    return UrlValidator.isHttpUrl(artwork) ? artwork : "";
  }

  private static String sanitize(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String normalized = value.replaceAll("\\p{Cntrl}", "").trim();
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }
}
