package net.dyhntastic.radio.core.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlValidator {

  private UrlValidator() {
  }

  public static boolean isHttpUrl(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      URI uri = new URI(value.trim());
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          && uri.getHost() != null
          && !uri.getHost().isBlank()
          && uri.getUserInfo() == null;
    } catch (URISyntaxException exception) {
      return false;
    }
  }

  public static boolean isOptionalHttpUrl(String value) {
    return value == null || value.isBlank() || isHttpUrl(value);
  }
}
