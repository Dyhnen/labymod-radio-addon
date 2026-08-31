# Unterstützte Versionen

Das Projekt verwendet die Minecraft-Versionen des lokal vorhandenen offiziellen
LabyMod-4-Addon-Templates und ergänzt die inzwischen offiziell unterstützte Version 26.2
(`gradle.properties`):

`1.8.9`, `1.12.2`, `1.16.5`, `1.17.1`, `1.18.2`, `1.19.4`, `1.20.1`, `1.20.4`,
`1.20.6`, `1.21`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`,
`1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`.

Minecraft Java Edition 26.2 wurde am 16. Juni 2026 veröffentlicht. LabyMod unterstützt die
Release-Version offiziell seit LabyMod 4.5.9. Das Ziel `26.2` ist deshalb zusätzlich in
`gradle.properties` registriert.

Die Addon-Metadaten verwenden wie das Template `minecraftVersion = "*"`. Der Code liegt in den
versionsunabhängigen `api`-/`core`-Modulen und greift nicht direkt auf Minecraft-/NMS-Klassen zu.

Diese Liste beschreibt die vom Template konfigurierten Build-Ziele. Mit dem Game Runner wurde
Minecraft `1.21.11` bis in eine lokale Welt gestartet; dabei wurden Addon-Initialisierung und
Ressourcenregistrierung im Log bestätigt. Settings, Activity, Widget-Editor und Audio wurden
dabei nicht interaktiv bedient. Die übrigen Versionen sind Build-Ziele, aber nicht praktisch
gestartet worden. Die Release-JAR verwendet ausschließlich die versionsunabhängigen LabyMod-4-
APIs aus `api` und `core`; `26.2` wird außerdem als LabyGradle-Client-Task verifiziert.
