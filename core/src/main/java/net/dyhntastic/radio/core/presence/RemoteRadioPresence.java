package net.dyhntastic.radio.core.presence;

import java.util.UUID;
import net.dyhntastic.radio.api.PlayerState;

/** Validated now-playing information received from another addon user. */
public record RemoteRadioPresence(
    UUID playerId,
    String station,
    String title,
    String artist,
    String artworkUrl,
    PlayerState state,
    long receivedAtMillis
) {
}
