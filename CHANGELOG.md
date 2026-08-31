# Changelog

Alle relevanten Änderungen am Dyhnunity Radio Player werden in dieser Datei dokumentiert.

## 1.0.7 – 2026-08-31

### Review-Bereitschaft

- Sämtliche produktiven Java-Streams, eigene Reflection und alle gefundenen veralteten
  LabyMod-API-Aufrufe wurden entfernt.
- Ein automatischer `reviewAudit` verhindert künftig verbotene Stream-/Reflection-Nutzung,
  direkte Konsolenausgabe, Core-Package-Zugriffe und fehlende Build-Sicherheitsdateien.
- Deutsche und englische Übersetzungen werden durch einen automatisierten Paritätstest geprüft.
- Datenschutzdokumentation, Flint-Einreichungswerte, manueller Produktions-Smoke-Test sowie ein
  fertiges Store-Icon und Thumbnail wurden ergänzt.

### Leistung und Sicherheit

- Header- und Nametag-Inhalte werden nur noch bei tatsächlichen Zustandsänderungen neu aufgebaut.
- Such- und Cover-Caches besitzen feste Obergrenzen, damit lange Sessions keinen unbegrenzten
  Speicherverbrauch verursachen.
- Extern gelieferte Cover-Adressen werden auf HTTP(S) begrenzt; eigene Sendernamen und URLs haben
  sinnvolle Längenlimits.
- Rohe technische Netzwerk- und Decoderfehler wurden durch vollständig lokalisierte Meldungen
  ersetzt; der User-Agent enthält keine veraltete Versionsnummer oder Spielerkennung.

### Build

- Addon-Version und Produktionsmetadaten wurden auf 1.0.7 angehoben.
- Der Autor in den Addon-Metadaten ist jetzt konsistent als DyhnenTv angegeben.

## 1.0.6 – 2026-08-31

### Geändert

- Der Bereich „Deine DyhnunityFM-Favoriten“ heißt jetzt neutral „Deine Favoriten“ beziehungsweise
  „Your Favorites“, da dort Sender aus allen Quellen gespeichert werden können.
- Beim Sneaken wird die komplette Now-Playing-Card ausgeblendet, statt Cover und leeren Hintergrund
  ohne Text stehen zu lassen.

### Behoben

- Entity-Tags verwenden ausschließlich die verifizierte Spieler-UUID aus dem LabyMod-Snapshot.
- Mobs können beim Anvisieren oder bei räumlicher Nähe nicht mehr fälschlich die eigene
  Now-Playing-Card erhalten.

## 1.0.5 – 2026-08-30

### Hinzugefügt

- Dreistufige Streamformat-Erkennung anhand von URL, HTTP-Content-Type und tatsächlicher
  Streamsignatur.
- Erkennung von MP3, AAC/AAC+, OGG/Opus, FLAC, WAV, HLS, M3U/PLS und HTML-Fehlerseiten.
- Deutsche und englische Hinweise sowie LabyMod-Benachrichtigungen für inkompatible Streams.
- Automatisierte Tests für URL-, Header- und Signaturerkennung.

### Geändert

- Radio Browser liefert weiterhin ausschließlich als MP3 deklarierte Sender.
- Die RadioReg-Filterung verwendet jetzt dieselbe zentrale Formatprüfung wie der Player.
- Unbekannte oder falsch deklarierte Formate werden probeweise zugelassen, damit funktionierende
  MP3-Streams ohne eindeutige Kennzeichnung nicht unnötig blockiert werden.
- Version und Produktionsmanifest wurden auf 1.0.5 aktualisiert.

### Behoben

- AAC-, HLS- oder Playlist-Streams werden nicht mehr kommentarlos an den MP3-Decoder übergeben.
- Eigene Sender mit einer eindeutig inkompatiblen Dateiendung werden bereits beim Speichern mit
  einer verständlichen Meldung abgelehnt.
- HTML-Antworten von fehlerhaften Streamservern werden als Webantwort statt als Audiodaten erkannt.

## 1.0.4 Preview 4 – 2026-08-30

### Hinzugefügt

- Synchronisierung der Now-Playing-Card über das in LabyMod integrierte LabyConnect.
- Freigabe wahlweise nur für LabyConnect-Freunde oder für andere Addon-Nutzer in der
  Spielumgebung.
- Darstellung der Now-Playing-Card über anderen Spielern, sofern beide das Addon verwenden und
  die Freigabe aktiviert haben.
- Heartbeat alle 15 Sekunden und automatisches Entfernen veralteter Cards nach 35 Sekunden.
- Validierung und Begrenzung aller empfangenen Presence-Daten.

### Datenschutz

- Die Freigabe ist standardmäßig deaktiviert.
- Übertragen werden nur Sendername, Titel, Interpret, Cover-URL und Wiedergabestatus.
- Stream-URLs, Favoriten und Addon-Einstellungen werden nicht übertragen.
- Es wird weder ein Minecraft-Serverplugin noch ein eigener Backend-Server benötigt.

### Behoben

- Stoppen, Deaktivieren oder Wechseln der Sichtbarkeit entfernt nicht mehr benötigte Cards.
- Beim Wechsel auf „nur Freunde“ werden bereits empfangene Cards von Nicht-Freunden sofort
  ausgeblendet.
- Empfangene Cards bleiben sichtbar, wenn der lokale Spieler selbst keinen Sender abspielt.

## 1.0.4 Preview 3 – 2026-08-30

### Hinzugefügt

- Neues neutrales Standard-Radiocover für Sender ohne eigenes Logo oder Cover.
- Eigene Play- und Pause-Symbole für eine eindeutig erkennbare Wiedergabesteuerung.
- Klare Anzeige, wenn ein Sender keine Live-Titelinformationen bereitstellt.

### Geändert

- Der Hauptbutton wechselt dynamisch zwischen Abspielen, Pause und Fortsetzen.
- Der separate Stop-Button wurde entfernt; die zentrale Wiedergabesteuerung übernimmt den
  vollständigen Playerzustand.
- Das neutrale Fallback-Cover ersetzt das DyhnunityFM-Markenlogo bei fremden oder unbekannten
  Sendern.

### Behoben

- Der Header zeigt bei einem ausgewählten oder laufenden Sender nicht länger fälschlich
  „Bereit für deinen Sound“ an.
- Metadatenkarten bleiben nach einer abgeschlossenen, erfolglosen Prüfung nicht dauerhaft bei
  „Titel wird geladen“ stehen.
- Wiedergabe aus einem Fehler- oder Stoppzustand baut den aktuellen Sender wieder vollständig auf.
