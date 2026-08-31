package net.dyhntastic.radio.core;

import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.VolumeSliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget.DropdownSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;

@ConfigName("settings")
public class RadioConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @ButtonSetting
  @SettingSection("radio")
  public void openRadio() {
    DyhnunityRadioAddon addon = DyhnunityRadioAddon.instance();
    if (addon != null) {
      addon.openRadio();
    }
  }

  @KeyBindSetting(acceptMouseButtons = false)
  private final ConfigProperty<Key> openRadioKey = new ConfigProperty<>(Key.F8);

  @VolumeSliderSetting(min = 0, max = 100, steps = 1)
  private final ConfigProperty<Float> volume = new ConfigProperty<>(60.0F);

  @SwitchSetting
  private final ConfigProperty<Boolean> rememberLastStation = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> autoPlayLastStation = new ConfigProperty<>(false);

  @DropdownSetting
  @SettingSection("playerName")
  private final ConfigProperty<PlayerNameDisplayPosition> playerCardPosition =
      ConfigProperty.createEnum(PlayerNameDisplayPosition.ABOVE_NAME);

  @SliderSetting(min = 50, max = 150, steps = 5)
  private final ConfigProperty<Float> playerCardScale = new ConfigProperty<>(100.0F);

  @SwitchSetting
  @SettingSection("widget")
  private final ConfigProperty<Boolean> widgetEnabled = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> widgetShowStation = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> widgetShowTitle = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> widgetShowArtist = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> widgetShowVolume = new ConfigProperty<>(true);

  @SwitchSetting
  @SettingSection("presence")
  private final ConfigProperty<Boolean> presenceEnabled = new ConfigProperty<>(false);

  @DropdownSetting
  private final ConfigProperty<PresencePrivacy> presencePrivacy =
      ConfigProperty.createEnum(PresencePrivacy.FRIENDS);

  private final ConfigProperty<String> favoritesJson = new ConfigProperty<>("");
  private final ConfigProperty<String> customStationsJson = new ConfigProperty<>("");
  private final ConfigProperty<String> lastStationJson = new ConfigProperty<>("");

  @ButtonSetting
  @SettingSection("dyhntastic")
  public void website() {
    Laby.labyAPI().minecraft().chatExecutor().openUrl("https://dyhntastic.net");
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Float> volume() {
    return this.volume;
  }

  public ConfigProperty<Key> openRadioKey() {
    return this.openRadioKey;
  }

  public ConfigProperty<Boolean> rememberLastStation() {
    return this.rememberLastStation;
  }

  public ConfigProperty<Boolean> autoPlayLastStation() {
    return this.autoPlayLastStation;
  }

  public ConfigProperty<PlayerNameDisplayPosition> playerCardPosition() {
    return this.playerCardPosition;
  }

  public ConfigProperty<Float> playerCardScale() {
    return this.playerCardScale;
  }

  public ConfigProperty<Boolean> widgetEnabled() {
    return this.widgetEnabled;
  }

  public ConfigProperty<Boolean> widgetShowStation() {
    return this.widgetShowStation;
  }

  public ConfigProperty<Boolean> widgetShowTitle() {
    return this.widgetShowTitle;
  }

  public ConfigProperty<Boolean> widgetShowArtist() {
    return this.widgetShowArtist;
  }

  public ConfigProperty<Boolean> widgetShowVolume() {
    return this.widgetShowVolume;
  }

  public ConfigProperty<Boolean> presenceEnabled() {
    return this.presenceEnabled;
  }

  public ConfigProperty<PresencePrivacy> presencePrivacy() {
    return this.presencePrivacy;
  }

  public ConfigProperty<String> favoritesJson() {
    return this.favoritesJson;
  }

  public ConfigProperty<String> customStationsJson() {
    return this.customStationsJson;
  }

  public ConfigProperty<String> lastStationJson() {
    return this.lastStationJson;
  }
}
