package net.dyhntastic.radio.core.hud;

import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.RadioRuntime;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine.State;

public final class RadioHudWidget extends TextHudWidget<RadioHudWidgetConfig> {

  private final RadioRuntime runtime;
  private TextLine stationLine;
  private TextLine titleLine;
  private TextLine artistLine;
  private TextLine volumeLine;

  public RadioHudWidget(RadioRuntime runtime) {
    super("radio", "Dyhnunity Radio Player", RadioHudWidgetConfig.class);
    this.runtime = runtime;
  }

  @Override
  public void load(RadioHudWidgetConfig config) {
    super.load(config);
    this.stationLine = this.createLine(
        Component.translatable("dyhnunity-radio.hudWidget.radio.labels.station"), "-"
    );
    this.titleLine = this.createLine(
        Component.translatable("dyhnunity-radio.hudWidget.radio.labels.title"), "-"
    );
    this.artistLine = this.createLine(
        Component.translatable("dyhnunity-radio.hudWidget.radio.labels.artist"), "-"
    );
    this.volumeLine = this.createLine(
        Component.translatable("dyhnunity-radio.hudWidget.radio.labels.volume"), "60%"
    );
  }

  @Override
  public void updateTextContent() {
    if (this.runtime == null || this.stationLine == null) {
      return;
    }
    RadioStation station = this.runtime.currentStation();
    RadioMetadata metadata = station == null ? RadioMetadata.EMPTY : station.metadata();
    String stationName = station == null ? "-" : station.name();
    String title = station == null || metadata.title().isBlank() ? "-" : metadata.title();
    String artist = metadata.artist().isBlank() ? "-" : metadata.artist();
    updateLine(
        this.stationLine,
        this.runtime.configuration().widgetShowStation().get(),
        stationName
    );
    updateLine(this.titleLine, this.runtime.configuration().widgetShowTitle().get(), title);
    updateLine(this.artistLine, this.runtime.configuration().widgetShowArtist().get(), artist);
    updateLine(
        this.volumeLine,
        this.runtime.configuration().widgetShowVolume().get(),
        Math.round(this.runtime.player().volume() * 100.0F) + "%"
    );
  }

  @Override
  public boolean isVisibleInGame() {
    return this.runtime != null
        && this.runtime.configuration().widgetEnabled().get()
        && super.isVisibleInGame();
  }

  private static void updateLine(TextLine line, boolean visible, Object value) {
    State nextState = visible ? State.VISIBLE : State.HIDDEN;
    if (line.state() != nextState) {
      line.setState(nextState);
    }
    line.updateAndFlush(value);
  }
}
