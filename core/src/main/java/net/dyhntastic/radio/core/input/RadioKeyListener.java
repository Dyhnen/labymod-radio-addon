package net.dyhntastic.radio.core.input;

import net.dyhntastic.radio.core.DyhnunityRadioAddon;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

public final class RadioKeyListener {

  private final DyhnunityRadioAddon addon;

  public RadioKeyListener(DyhnunityRadioAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    if (event.state() != KeyEvent.State.PRESS) {
      return;
    }
    Key configuredKey = this.addon.configuration().openRadioKey().get();
    if (configuredKey == null || configuredKey == Key.NONE || !configuredKey.equals(event.key())) {
      return;
    }
    this.addon.openRadio();
  }
}
