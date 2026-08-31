package net.dyhntastic.radio.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.provider.DyhnunityStations;
import org.junit.jupiter.api.Test;

class StationRepositoryTest {

  @Test
  void persistsFavoritesAndCustomStations() {
    AtomicReference<String> favorites = new AtomicReference<>();
    AtomicReference<String> custom = new AtomicReference<>();
    List<RadioStation> defaultStations = DyhnunityStations.defaultFavorites();
    StationRepository repository = new StationRepository(
        "", "", defaultStations, (favoritesJson, customJson) -> {
          favorites.set(favoritesJson);
          custom.set(customJson);
        }
    );
    RadioStation own = station("custom:test", RadioSource.CUSTOM);

    repository.saveCustom(own);
    repository.toggleFavorite(own);

    assertEquals(9, repository.favorites().size() - 1);
    assertTrue(repository.isFavorite(defaultStations.getFirst().id()));
    assertTrue(repository.isFavorite(own.id()));
    assertEquals(1, repository.customStations().size());

    StationRepository restored = new StationRepository(
        favorites.get(), custom.get(), defaultStations, (ignoredA, ignoredB) -> { }
    );
    assertTrue(restored.isFavorite(own.id()));
    assertEquals(own.streamUrl(), restored.customStations().getFirst().streamUrl());
  }

  @Test
  void migratesCorruptLegacyJsonToDefaultStation() {
    List<RadioStation> defaultStations = DyhnunityStations.defaultFavorites();
    StationRepository repository = new StationRepository(
        "{broken", "old-format", defaultStations, (ignoredA, ignoredB) -> { }
    );

    assertEquals(defaultStations, repository.favorites());
    assertTrue(repository.customStations().isEmpty());
  }

  @Test
  void deletingCustomAlsoRemovesFavorite() {
    List<RadioStation> defaultStations = DyhnunityStations.defaultFavorites();
    StationRepository repository = new StationRepository(
        "", "", defaultStations, (ignoredA, ignoredB) -> { }
    );
    RadioStation own = station("custom:test", RadioSource.CUSTOM);
    repository.saveCustom(own);
    repository.toggleFavorite(own);
    repository.deleteCustom(own.id());

    assertFalse(repository.isFavorite(own.id()));
    assertTrue(repository.customStations().isEmpty());
  }

  private static RadioStation station(String id, RadioSource source) {
    return new RadioStation(
        id, "Station", source, "https://radio.example/live.mp3", "", "",
        List.of(), "", "", "", RadioMetadata.EMPTY
    );
  }
}
