# Dyhnunity Radio Player

Dyhnunity Radio Player ist ein clientseitiges LabyMod-4-Addon von **DyhnenTv**. Es integriert
DyhnunityFM, laut.fm, Radio Browser, I LOVE MUSIC, RadioReg und eigene HTTP(S)-Radiostreams in
eine native LabyMod Activity und in den HUD-Widget-Editor.

## Features

- echte LabyMod-4-Addon-Metadaten und `@AddonMain`-Entry-Point
- sichtbarer Logo-Button direkt im Minecraft-Hauptmenü
- Radio-Activity per konfigurierbarem Hotkey (Standard: `F8`) aus Spiel und Hauptmenü
- HUD-Textwidget für Sender, Titel, Artist und Lautstärke
- asynchrone laut.fm-Suche, Discovery, Genres und Current/Next-Artist-Metadaten
- RadioReg-Discovery, clientseitige Suche/Tags und Current/Next/History-Metadaten
- persistierte Favoriten-Snapshots für Offline-Anzeige
- eigene Sender anlegen, bearbeiten, löschen, favorisieren und abspielen
- 400-ms-Such-Debounce, TTL-Caches und Session-IDs gegen veraltete Antworten
- optionale Now-Playing-Cards anderer Addon-Nutzer über LabyConnect, ohne Serverplugin

## Installation

Die von `createReleaseJar` erzeugte Datei `build/libs/dyhnunity-radio-player-release.jar` in den
LabyMod-4-Addons-Ordner kopieren und LabyMod neu starten. Keine JAR aus `api/build/libs`
oder `core/build/libs` verwenden.

## Build

Java 21 oder neuer ist erforderlich:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew tasks
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew reviewAudit
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew createReleaseJar
```

Das lokal vorhandene Template weist `createReleaseJar` als Release-Task aus. Das installierbare
Ergebnis liegt anschließend unter `build/libs/dyhnunity-radio-player-release.jar`.

## Store-Review

`reviewAudit` prüft bei jedem Build die von LabyMod ausdrücklich verbotene Stream-/Reflection-
Nutzung, direkte Konsolenausgabe, Core-Package-Zugriffe, Legacy-Farbcodes sowie Workflow und
Gradle-Wrapper-Checksumme. Datenschutzangaben stehen in [PRIVACY.md](PRIVACY.md); vorausgefüllte
Flint-Werte, Store-Assets und der Produktions-Smoke-Test stehen in
[STORE_SUBMISSION.md](STORE_SUBMISSION.md).

## DyhnunityFM

Beim ersten Start sind neun offizielle DyhnunityFM-Sender als direkt abspielbare Favoriten
vorhanden: Main Station, Dyhntastic, Gaming, Chillout, Party, Schlager, Rap, Bass und Electro.
Sie verwenden ihre verifizierten laut.fm-Stream-URLs und Senderlogos. Der Sender „Dyhntastic“
wirbt sichtbar für das Minecraft Server Network auf Dyhntastic.net.

## laut.fm

Verwendete öffentliche Endpunkte: `/stations`, `/search/stations`, `/genres`,
`/station/{name}`, `/current_song` und `/next_artists`. Suchergebnisse werden entdoppelt und
gecached.

## RadioReg

Verwendete öffentliche Endpunkte: `/stream` und `/stream/{id}/history`. Da die offizielle
Dokumentation keinen öffentlichen Suchendpunkt ausweist, filtert das Addon die gecachte aktive
Streamliste nach Name, Land und Tags.

## Custom Stations und Favoriten

Eigene Sender benötigen nur einen Namen und eine gültige HTTP(S)-Stream-URL. Favoriten und eigene
Sender werden in der LabyMod-Addon-Konfiguration gespeichert.

## Audio

MP3-Streams werden über JLayer (LGPL-2.1), als offizielle LabyMod-Addon-Maven-Abhängigkeit
deklariert, auf einem eigenen Audiothread wiedergegeben. Der Player prüft URL, HTTP-Content-Type
und die tatsächliche Streamsignatur. AAC/AAC+, OGG/Opus, FLAC, WAV, HLS, M3U/PLS und versehentlich
gelieferte HTML-Seiten werden vor dem MP3-Decoder erkannt und zweisprachig als inkompatibel
gemeldet. Unbekannte Formate werden probeweise zugelassen, damit falsch deklarierte MP3-Streams
weiter funktionieren.

## Widgets

„Dyhnunity Radio Player“ erscheint im LabyMod Widget Editor. Sender, Titel, Artist und Lautstärke
lassen sich einzeln ein- oder ausblenden.

## Privacy und Presence

Die LabyConnect-Freigabe ist standardmäßig deaktiviert. Nach ausdrücklicher Aktivierung kann sie
wahlweise nur LabyConnect-Freunden oder anderen Addon-Nutzern in der Spielumgebung Sender, Titel,
Interpret, Cover und Wiedergabestatus übermitteln. Stream-URLs, Favoriten und Einstellungen werden
nicht übertragen. Ein Heartbeat aktualisiert die Card; bei Stop, Deaktivierung oder ausbleibenden
Updates wird sie automatisch entfernt. Es ist weder ein Serverplugin noch ein eigener Backend-Server
erforderlich. Beide Spieler benötigen das Addon und eine aktive LabyConnect-Verbindung.

## Troubleshooting

- Stream startet nicht: prüfen, ob die URL direkt einen MP3-Stream statt HTML/HLS liefert.
- API offline: Favoriten und eigene Sender bleiben aus den lokalen Snapshots verfügbar.
- Kein Ton: Betriebssystem-Ausgabegerät und Java-Sound-Mixer prüfen.

Radio: [DyhnunityFM.de](https://dyhnunityfm.de) · Website: [Dyhntastic.net](https://dyhntastic.net)
