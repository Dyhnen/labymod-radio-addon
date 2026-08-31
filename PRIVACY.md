# Privacy

Dyhnunity Radio Player has no analytics, advertising SDK, account system, or operator-owned
backend. It does not transmit Minecraft session tokens, the player's Minecraft UUID, favorites,
custom stations, or addon settings to the radio catalog providers.

## Network connections

- Opening the discovery page requests public station data from laut.fm, Radio Browser,
  I LOVE MUSIC, and RadioReg.
- Starting a station connects directly to the selected station's HTTP(S) MP3 stream.
- Station and album artwork is fetched from the image URL supplied by the selected catalog.
- As with every internet connection, the destination can observe ordinary connection data such
  as the user's IP address and the addon's generic User-Agent. The User-Agent contains no player
  identifier.

## Optional LabyConnect presence

Now Playing sharing is disabled by default and requires an explicit setting change. When enabled,
the user chooses between LabyConnect friends and nearby LabyConnect users. The addon then shares
only the station name, song title, artist, artwork URL, playback state, and payload version through
LabyConnect. Stream URLs, favorites, custom stations, and configuration values are never included.
The payload is cleared when playback stops, sharing is disabled, or the addon is deactivated.

## Local storage

Favorites, custom station names and URLs, the last selected station, and addon preferences are
stored only through LabyMod's addon configuration system on the user's device. Disabling the addon
stops its player, network executors, scheduled metadata refresh, LabyConnect presence, HUD widget,
and name-tag integration.

---

# Datenschutz

Der Dyhnunity Radio Player verwendet keine Analyse- oder Werbe-SDKs, kein eigenes Kontosystem und
kein vom Betreiber bereitgestelltes Backend. Minecraft-Sitzungstoken, UUID, Favoriten, eigene
Sender und Einstellungen werden nicht an die Radiokataloge übertragen.

Beim Öffnen der Sendersuche werden öffentliche Daten von laut.fm, Radio Browser, I LOVE MUSIC und
RadioReg geladen. Die Wiedergabe verbindet sich direkt mit dem ausgewählten HTTP(S)-MP3-Stream;
Cover werden von der durch den Katalog gelieferten Bildadresse geladen. Wie bei jeder
Internetverbindung kann das Ziel normale Verbindungsdaten wie die IP-Adresse und den generischen,
nicht personenbezogenen User-Agent sehen.

Die LabyConnect-Freigabe ist standardmäßig ausgeschaltet. Nach ausdrücklicher Aktivierung werden
je nach gewählter Sichtbarkeit ausschließlich Sendername, Titel, Interpret, Cover-Adresse,
Wiedergabestatus und Protokollversion über LabyConnect geteilt. Stream-URLs, Favoriten, eigene
Sender und Einstellungen werden nicht übertragen. Lokale Daten verbleiben in der
LabyMod-Addon-Konfiguration auf dem Gerät.
