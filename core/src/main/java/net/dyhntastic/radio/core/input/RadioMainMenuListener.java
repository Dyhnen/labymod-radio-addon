package net.dyhntastic.radio.core.input;

import net.dyhntastic.radio.core.DyhnunityRadioAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.gui.screen.IngameMenuInitializeEvent;
import net.labymod.api.event.client.gui.screen.MainMenuInitializeEvent;

public final class RadioMainMenuListener {

  private static final ResourceLocation RADIO_ICON = ResourceLocation.create(
      "dyhnunity-radio", "textures/icon.png"
  );

  private final DyhnunityRadioAddon addon;

  public RadioMainMenuListener(DyhnunityRadioAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onMainMenuInitialize(MainMenuInitializeEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }
    event.addIconButton(
        Icon.texture(RADIO_ICON),
        Component.translatable("dyhnunity-radio.ui.mainMenuButton"),
        this.addon::openRadio
    );
  }

  @Subscribe
  public void onIngameMenuInitialize(IngameMenuInitializeEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }
    event.addRightButton(
        Component.translatable("dyhnunity-radio.ui.pauseMenuButton"),
        Icon.texture(RADIO_ICON),
        this.addon::openRadio
    );
  }
}
