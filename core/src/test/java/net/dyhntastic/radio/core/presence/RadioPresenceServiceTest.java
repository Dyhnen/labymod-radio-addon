package net.dyhntastic.radio.core.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.PresencePrivacy;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.event.labymod.labyconnect.session.LabyConnectBroadcastEvent;
import org.junit.jupiter.api.Test;

class RadioPresenceServiceTest {

  @Test
  void payloadContainsOnlyPublicNowPlayingData() {
    RadioStation station = station();

    JsonObject payload = RadioPresenceService.createPayload(station, PlayerState.PLAYING);

    assertEquals("Test Radio", payload.get("station").getAsString());
    assertEquals("Title", payload.get("title").getAsString());
    assertEquals("Artist", payload.get("artist").getAsString());
    assertEquals("https://images.example/cover.jpg", payload.get("artwork").getAsString());
    assertFalse(payload.toString().contains(station.streamUrl()));
    assertFalse(payload.toString().contains(station.id()));
  }

  @Test
  void decodesValidPayloadAndRejectsClearOrStoppedPayloads() {
    UUID sender = UUID.randomUUID();
    JsonObject payload = RadioPresenceService.createPayload(station(), PlayerState.PAUSED);

    RemoteRadioPresence presence = RadioPresenceService.decode(sender, payload, 123L);

    assertEquals(sender, presence.playerId());
    assertEquals("Test Radio", presence.station());
    assertEquals(PlayerState.PAUSED, presence.state());
    assertEquals(123L, presence.receivedAtMillis());
    assertNull(RadioPresenceService.decode(sender, RadioPresenceService.clearPayload(), 124L));

    payload.addProperty("state", PlayerState.STOPPED.name());
    assertNull(RadioPresenceService.decode(sender, payload, 125L));
  }

  @Test
  void rejectsUnknownVersionAndUnsafeArtwork() {
    UUID sender = UUID.randomUUID();
    JsonObject payload = RadioPresenceService.createPayload(station(), PlayerState.PLAYING);
    payload.addProperty("artwork", "file:///etc/passwd");

    RemoteRadioPresence presence = RadioPresenceService.decode(sender, payload, 1L);
    assertEquals("", presence.artworkUrl());

    payload.addProperty("v", 99);
    assertNull(RadioPresenceService.decode(sender, payload, 2L));
  }

  @Test
  void receivedPresenceExpiresWithoutAnotherHeartbeat() {
    AtomicLong clock = new AtomicLong(1_000L);
    RadioPresenceService service = new RadioPresenceService(
        null,
        new ConfigProperty<>(true),
        ConfigProperty.createEnum(PresencePrivacy.OTHER_USERS),
        clock::get
    );
    UUID sender = UUID.randomUUID();
    service.onBroadcast(new LabyConnectBroadcastEvent(
        null,
        LabyConnectBroadcastEvent.Action.RECEIVE,
        sender,
        RadioPresenceService.BROADCAST_KEY,
        RadioPresenceService.createPayload(station(), PlayerState.PLAYING)
    ));

    assertEquals("Test Radio", service.remotePresence(sender).station());
    clock.addAndGet(RadioPresenceService.REMOTE_TIMEOUT_MILLIS + 1L);
    assertNull(service.remotePresence(sender));
  }

  private static RadioStation station() {
    return new RadioStation(
        "laut:test",
        "Test Radio",
        RadioSource.LAUTFM,
        "https://radio.example/private-live.mp3",
        "",
        "https://images.example/station.jpg",
        List.of(),
        "",
        "",
        "",
        new RadioMetadata("Title", "Artist", "https://images.example/cover.jpg", "", "")
    );
  }
}
