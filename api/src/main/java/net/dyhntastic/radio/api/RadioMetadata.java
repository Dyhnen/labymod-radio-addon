package net.dyhntastic.radio.api;

public record RadioMetadata(
    String title,
    String artist,
    String coverUrl,
    String nextTitle,
    String nextArtist
) {

  public static final RadioMetadata EMPTY = new RadioMetadata("", "", "", "", "");

  public RadioMetadata {
    title = normalize(title);
    artist = normalize(artist);
    coverUrl = normalize(coverUrl);
    nextTitle = normalize(nextTitle);
    nextArtist = normalize(nextArtist);
  }

  private static String normalize(String value) {
    return value == null ? "" : value;
  }
}
