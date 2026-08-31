package net.dyhntastic.radio.core.audio;

/** Audio and playlist formats relevant to the MP3-only JLayer player. */
public enum StreamFormat {
  MP3("MP3", true),
  AAC("AAC/AAC+", false),
  OGG("Ogg/Opus", false),
  FLAC("FLAC", false),
  HLS("HLS", false),
  PLAYLIST("M3U/PLS", false),
  WAV("WAV", false),
  WEB_PAGE("HTML", false),
  UNKNOWN("", true);

  private final String displayName;
  private final boolean playable;

  StreamFormat(String displayName, boolean playable) {
    this.displayName = displayName;
    this.playable = playable;
  }

  public String displayName() {
    return this.displayName;
  }

  /** Unknown streams are allowed through so incorrectly labelled MP3 servers keep working. */
  public boolean canAttemptPlayback() {
    return this.playable;
  }
}
