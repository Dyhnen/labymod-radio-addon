package net.dyhntastic.radio.core.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioPlayer;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.RadioConstants;
import net.dyhntastic.radio.core.util.UrlValidator;

public final class JLayerRadioPlayer implements RadioPlayer {

  public static final String UNSUPPORTED_FORMAT_ERROR = "unsupported-format:";
  public static final String INVALID_STREAM_ERROR = "invalid-stream-url";
  public static final String PLAYBACK_FAILED_ERROR = "playback-failed";
  private final ExecutorService audioExecutor;
  private final AtomicLong generation = new AtomicLong();
  private final List<Consumer<PlayerState>> listeners = new CopyOnWriteArrayList<>();
  private volatile PlayerState state = PlayerState.STOPPED;
  private volatile RadioStation currentStation;
  private volatile float volume = 0.6F;
  private volatile String errorMessage = "";
  private volatile Future<?> playbackTask;
  private volatile InputStream input;
  private volatile SourceDataLine line;

  public JLayerRadioPlayer(ExecutorService audioExecutor) {
    this.audioExecutor = audioExecutor;
  }

  @Override
  public synchronized void play(RadioStation station) {
    this.currentStation = station;
    this.start(station);
  }

  @Override
  public synchronized void pause() {
    if (this.state != PlayerState.PLAYING && this.state != PlayerState.LOADING) {
      return;
    }
    this.generation.incrementAndGet();
    this.closeActiveResources();
    this.setState(PlayerState.PAUSED);
  }

  @Override
  public synchronized void resume() {
    if (this.state == PlayerState.PAUSED && this.currentStation != null) {
      this.start(this.currentStation);
    }
  }

  @Override
  public synchronized void stop() {
    this.generation.incrementAndGet();
    this.closeActiveResources();
    this.currentStation = null;
    this.errorMessage = "";
    this.setState(PlayerState.STOPPED);
  }

  @Override
  public synchronized void reconnect() {
    if (this.currentStation != null) {
      this.start(this.currentStation);
    }
  }

  @Override
  public void switchStation(RadioStation station) {
    this.play(station);
  }

  @Override
  public void setVolume(float volume) {
    this.volume = Math.max(0.0F, Math.min(1.0F, volume));
  }

  @Override
  public float volume() {
    return this.volume;
  }

  @Override
  public PlayerState state() {
    return this.state;
  }

  @Override
  public RadioStation currentStation() {
    return this.currentStation;
  }

  @Override
  public RadioMetadata metadata() {
    RadioStation station = this.currentStation;
    return station == null ? RadioMetadata.EMPTY : station.metadata();
  }

  @Override
  public long sessionId() {
    return this.generation.get();
  }

  @Override
  public String errorMessage() {
    return this.errorMessage;
  }

  @Override
  public void addStateListener(Consumer<PlayerState> listener) {
    this.listeners.add(listener);
  }

  @Override
  public synchronized void close() {
    this.stop();
  }

  private synchronized void start(RadioStation station) {
    long session = this.generation.incrementAndGet();
    this.closeActiveResources();
    this.errorMessage = "";
    if (!UrlValidator.isHttpUrl(station.streamUrl())) {
      this.errorMessage = INVALID_STREAM_ERROR;
      this.setState(PlayerState.ERROR);
      return;
    }
    StreamFormat urlFormat = StreamFormatDetector.fromUrl(station.streamUrl());
    if (!urlFormat.canAttemptPlayback()) {
      this.errorMessage = unsupportedFormatError(urlFormat);
      this.setState(PlayerState.ERROR);
      return;
    }
    this.setState(PlayerState.LOADING);
    this.playbackTask = this.audioExecutor.submit(() -> this.decode(session, station));
  }

  private void decode(long session, RadioStation station) {
    try {
      HttpURLConnection connection = (HttpURLConnection) URI.create(station.streamUrl()).toURL().openConnection();
      connection.setConnectTimeout(10_000);
      connection.setReadTimeout(20_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", RadioConstants.USER_AGENT);
      connection.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.8,*/*;q=0.1");
      BufferedInputStream stream = new BufferedInputStream(
          connection.getInputStream(),
          64 * 1024
      );
      this.input = stream;
      stream.mark(64);
      byte[] prefix = new byte[32];
      int prefixLength = stream.read(prefix);
      stream.reset();
      StreamFormat detectedFormat = StreamFormatDetector.detect(
          connection.getURL().toString(),
          connection.getContentType(),
          prefix,
          prefixLength
      );
      if (!detectedFormat.canAttemptPlayback()) {
        throw new IllegalStateException(unsupportedFormatError(detectedFormat));
      }
      Bitstream bitstream = new Bitstream(stream);
      Decoder decoder = new Decoder();
      SourceDataLine outputLine = null;
      Header header;
      while (this.generation.get() == session && (header = bitstream.readFrame()) != null) {
        SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
        if (outputLine == null) {
          AudioFormat format = new AudioFormat(
              samples.getSampleFrequency(),
              16,
              samples.getChannelCount(),
              true,
              false
          );
          outputLine = (SourceDataLine) AudioSystem.getLine(
              new DataLine.Info(SourceDataLine.class, format)
          );
          outputLine.open(format);
          outputLine.start();
          this.line = outputLine;
          if (this.generation.get() == session) {
            this.setState(PlayerState.PLAYING);
          }
        }
        byte[] pcm = toPcm(samples.getBuffer(), samples.getBufferLength(), this.volume);
        outputLine.write(pcm, 0, pcm.length);
        bitstream.closeFrame();
      }
      if (outputLine != null) {
        outputLine.drain();
      }
      bitstream.close();
      if (this.generation.get() == session && this.state != PlayerState.PAUSED) {
        this.setState(PlayerState.STOPPED);
      }
    } catch (Exception exception) {
      if (this.generation.get() == session) {
        String message = exception.getMessage();
        this.errorMessage = message != null && message.startsWith(UNSUPPORTED_FORMAT_ERROR)
            ? message
            : PLAYBACK_FAILED_ERROR;
        this.setState(PlayerState.ERROR);
      }
    } finally {
      if (this.generation.get() == session) {
        this.closeActiveResources();
      }
    }
  }

  private static byte[] toPcm(short[] samples, int length, float volume) {
    byte[] bytes = new byte[length * 2];
    for (int i = 0; i < length; i++) {
      int scaled = Math.round(samples[i] * volume);
      scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled));
      bytes[i * 2] = (byte) scaled;
      bytes[i * 2 + 1] = (byte) (scaled >>> 8);
    }
    return bytes;
  }

  private static String unsupportedFormatError(StreamFormat format) {
    return UNSUPPORTED_FORMAT_ERROR + format.displayName();
  }

  private synchronized void closeActiveResources() {
    Future<?> task = this.playbackTask;
    this.playbackTask = null;
    if (task != null) {
      task.cancel(true);
    }
    SourceDataLine activeLine = this.line;
    this.line = null;
    if (activeLine != null) {
      activeLine.stop();
      activeLine.flush();
      activeLine.close();
    }
    InputStream activeInput = this.input;
    this.input = null;
    if (activeInput != null) {
      try {
        activeInput.close();
      } catch (Exception ignored) {
      }
    }
  }

  private void setState(PlayerState next) {
    this.state = next;
    for (Consumer<PlayerState> listener : this.listeners) {
      listener.accept(next);
    }
  }
}
