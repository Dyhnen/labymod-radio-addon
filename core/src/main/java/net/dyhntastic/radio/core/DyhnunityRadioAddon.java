package net.dyhntastic.radio.core;

import net.dyhntastic.radio.core.hud.RadioHudWidget;
import net.dyhntastic.radio.core.input.RadioKeyListener;
import net.dyhntastic.radio.core.input.RadioMainMenuListener;
import net.dyhntastic.radio.core.nametag.RadioNameTag;
import net.dyhntastic.radio.core.ui.RadioActivity;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class DyhnunityRadioAddon extends LabyAddon<RadioConfiguration> {

  private static volatile DyhnunityRadioAddon instance;
  private RadioRuntime runtime;

  public DyhnunityRadioAddon() {
    instance = this;
  }

  public static DyhnunityRadioAddon instance() {
    return instance;
  }

  @Override
  protected void enable() {
    this.registerSettingCategory();
    this.runtime = new RadioRuntime(this, this.configuration());
    this.registerListener(this.runtime.presenceService());
    this.registerListener(new RadioKeyListener(this));
    this.registerListener(new RadioMainMenuListener(this));
    this.labyAPI().hudWidgetRegistry().register(() -> new RadioHudWidget(this.runtime));
    this.labyAPI().tagRegistry().register(
        "dyhnunity-radio-above",
        PositionType.ABOVE_NAME,
        new RadioNameTag(this.runtime, PlayerNameDisplayPosition.ABOVE_NAME)
    );
    this.labyAPI().tagRegistry().register(
        "dyhnunity-radio-below",
        PositionType.BELOW_NAME,
        new RadioNameTag(this.runtime, PlayerNameDisplayPosition.BELOW_NAME)
    );
    this.runtime.start();
    this.logger().info(
        "Dyhnunity Radio Player enabled with laut.fm, Radio Browser, I LOVE MUSIC and RadioReg providers"
    );
  }

  @Override
  protected void onDeactivated() {
    this.labyAPI().tagRegistry().unregister("dyhnunity-radio-above");
    this.labyAPI().tagRegistry().unregister("dyhnunity-radio-below");
    if (this.runtime != null) {
      this.runtime.close();
      this.runtime = null;
    }
    this.logger().info("Dyhnunity Radio Player stopped");
  }

  @Override
  protected Class<RadioConfiguration> configurationClass() {
    return RadioConfiguration.class;
  }

  public RadioRuntime runtime() {
    return this.runtime;
  }

  public void openRadio() {
    RadioRuntime activeRuntime = this.runtime;
    if (activeRuntime == null || !this.configuration().enabled().get()) {
      return;
    }
    this.labyAPI().minecraft().executeOnRenderThread(() -> {
      if (Laby.references().activityController().isActivityOpen(RadioActivity.class)) {
        return;
      }
      this.labyAPI().minecraft().minecraftWindow().displayScreen(new RadioActivity(activeRuntime));
    });
  }
}
