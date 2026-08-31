package net.dyhntastic.radio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LocalizationParityTest {

  @Test
  void germanAndEnglishExposeTheSameNonEmptyTranslationKeys() {
    JsonObject german = load("de_de.json");
    JsonObject english = load("en_us.json");
    Set<String> germanKeys = new LinkedHashSet<>();
    Set<String> englishKeys = new LinkedHashSet<>();
    collect(german, "", germanKeys);
    collect(english, "", englishKeys);
    assertEquals(germanKeys, englishKeys);
  }

  private static JsonObject load(String language) {
    String path = "assets/dyhnunity-radio/i18n/" + language;
    InputStream input = LocalizationParityTest.class.getClassLoader().getResourceAsStream(path);
    if (input == null) {
      throw new IllegalStateException("Missing localization " + path);
    }
    try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not read localization " + path, exception);
    }
  }

  private static void collect(JsonObject object, String parent, Set<String> keys) {
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String path = parent.isEmpty() ? entry.getKey() : parent + '.' + entry.getKey();
      JsonElement value = entry.getValue();
      if (value.isJsonObject()) {
        collect(value.getAsJsonObject(), path, keys);
      } else {
        assertFalse(value.isJsonNull(), "Null translation: " + path);
        assertFalse(value.getAsString().isBlank(), "Blank translation: " + path);
        keys.add(path);
      }
    }
  }
}
