package net.dyhntastic.radio.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.dyhntastic.radio.api.RadioStation;

public final class StationRepository {

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final Map<String, RadioStation> favorites = new LinkedHashMap<>();
  private final Map<String, RadioStation> customStations = new LinkedHashMap<>();
  private final BiConsumer<String, String> persistence;

  public StationRepository(
      String favoritesJson,
      String customStationsJson,
      List<RadioStation> defaultFavorites,
      BiConsumer<String, String> persistence
  ) {
    this.persistence = Objects.requireNonNull(persistence, "persistence");
    Objects.requireNonNull(defaultFavorites, "defaultFavorites");
    this.read(favoritesJson, this.favorites);
    this.read(customStationsJson, this.customStations);
    boolean changed = false;
    if (this.favorites.isEmpty()) {
      for (RadioStation station : defaultFavorites) {
        changed |= this.favorites.putIfAbsent(station.id(), station) == null;
      }
    } else {
      for (RadioStation station : defaultFavorites) {
        RadioStation previous = this.favorites.get(station.id());
        if (previous != null && !previous.equals(station)) {
          this.favorites.put(station.id(), station);
          changed = true;
        }
      }
    }
    if (changed) {
      this.persist();
    }
  }

  public synchronized List<RadioStation> favorites() {
    return List.copyOf(this.favorites.values());
  }

  public synchronized List<RadioStation> customStations() {
    return List.copyOf(this.customStations.values());
  }

  public synchronized boolean isFavorite(String stationId) {
    return this.favorites.containsKey(stationId);
  }

  public synchronized void toggleFavorite(RadioStation station) {
    if (this.favorites.remove(station.id()) == null) {
      this.favorites.put(station.id(), station);
    }
    this.persist();
  }

  public synchronized void updateFavoriteSnapshot(RadioStation station) {
    if (this.favorites.containsKey(station.id())) {
      this.favorites.put(station.id(), station);
      this.persist();
    }
  }

  public synchronized void saveCustom(RadioStation station) {
    this.customStations.put(station.id(), station);
    if (this.favorites.containsKey(station.id())) {
      this.favorites.put(station.id(), station);
    }
    this.persist();
  }

  public synchronized void deleteCustom(String stationId) {
    this.customStations.remove(stationId);
    this.favorites.remove(stationId);
    this.persist();
  }

  public synchronized RadioStation find(String stationId) {
    RadioStation custom = this.customStations.get(stationId);
    return custom != null ? custom : this.favorites.get(stationId);
  }

  private void read(String json, Map<String, RadioStation> destination) {
    if (json == null || json.isBlank()) {
      return;
    }
    try {
      RadioStation[] values = this.gson.fromJson(json, RadioStation[].class);
      if (values != null) {
        for (RadioStation station : values) {
          if (station != null && station.id() != null) {
            destination.put(station.id(), station);
          }
        }
      }
    } catch (JsonSyntaxException ignored) {
      destination.clear();
    }
  }

  private void persist() {
    this.persistence.accept(
        this.gson.toJson(new ArrayList<>(this.favorites.values())),
        this.gson.toJson(new ArrayList<>(this.customStations.values()))
    );
  }
}
