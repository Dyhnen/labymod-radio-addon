package net.dyhntastic.radio.api;

import java.util.function.Consumer;

public interface RadioPlayer extends AutoCloseable {

  void play(RadioStation station);

  void pause();

  void resume();

  void stop();

  void reconnect();

  void switchStation(RadioStation station);

  void setVolume(float volume);

  float volume();

  PlayerState state();

  RadioStation currentStation();

  RadioMetadata metadata();

  long sessionId();

  String errorMessage();

  void addStateListener(Consumer<PlayerState> listener);

  @Override
  void close();
}
