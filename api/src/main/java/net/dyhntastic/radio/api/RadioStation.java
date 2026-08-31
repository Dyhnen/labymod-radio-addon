package net.dyhntastic.radio.api;

import java.util.List;
import java.util.Objects;

public record RadioStation(
    String id,
    String name,
    RadioSource source,
    String streamUrl,
    String homepageUrl,
    String logoUrl,
    List<String> tags,
    String genre,
    String country,
    String language,
    RadioMetadata metadata
) {

  public RadioStation {
    id = Objects.requireNonNull(id, "id");
    name = Objects.requireNonNull(name, "name");
    source = Objects.requireNonNull(source, "source");
    streamUrl = normalize(streamUrl);
    homepageUrl = normalize(homepageUrl);
    logoUrl = normalize(logoUrl);
    tags = tags == null ? List.of() : List.copyOf(tags);
    genre = normalize(genre);
    country = normalize(country);
    language = normalize(language);
    metadata = metadata == null ? RadioMetadata.EMPTY : metadata;
  }

  public RadioStation withMetadata(RadioMetadata newMetadata) {
    return new RadioStation(
        this.id,
        this.name,
        this.source,
        this.streamUrl,
        this.homepageUrl,
        this.logoUrl,
        this.tags,
        this.genre,
        this.country,
        this.language,
        newMetadata
    );
  }

  private static String normalize(String value) {
    return value == null ? "" : value;
  }
}
