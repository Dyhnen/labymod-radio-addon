package net.dyhntastic.radio.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrlValidatorTest {

  @Test
  void acceptsOnlyHttpUrlsWithHostsAndWithoutCredentials() {
    assertTrue(UrlValidator.isHttpUrl("https://radio.example/live.mp3"));
    assertTrue(UrlValidator.isHttpUrl("http://localhost:8000/stream"));
    assertFalse(UrlValidator.isHttpUrl("file:///tmp/stream.mp3"));
    assertFalse(UrlValidator.isHttpUrl("https://user:secret@example.org/live"));
    assertFalse(UrlValidator.isHttpUrl("not a url"));
  }
}
