package net.dyhntastic.radio.core.nametag;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.PlayerNameDisplayPosition;
import net.dyhntastic.radio.core.RadioRuntime;
import net.dyhntastic.radio.core.presence.RemoteRadioPresence;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.EntityExtraKeys;
import net.labymod.api.client.render.state.entity.CustomAvatarDataSnapshot;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission;

/** Renders cover, title, and artist together as one now-playing card. */
public final class RadioNameTag extends ComponentNameTag {

  private static final TextColor TITLE_COLOR = TextColor.color(255, 83, 167);
  private static final TextColor ARTIST_COLOR = TextColor.color(203, 181, 230);
  private static final int CARD_COLOR = 0xED160A20;
  private static final int WHITE = 0xFFFFFFFF;
  private static final int PADDING = 3;
  private static final int COVER_SIZE = 20;
  private static final int COVER_GAP = 4;
  private static final int MAX_TEXT_WIDTH = 118;
  private static final List<Component> NO_COMPONENTS = List.of();
  private static final List<Component> CARD_COMPONENTS = List.of(Component.empty());

  private final RadioRuntime runtime;
  private final PlayerNameDisplayPosition position;
  private boolean visible;
  private Icon cover;
  private String coverKey = "";
  private Component title = Component.empty();
  private Component artist = Component.empty();
  private float titleWidth;
  private float artistWidth;
  private float cardWidth;
  private float cardHeight;
  private String resolvedTitle = "";
  private String resolvedArtist = "";
  private String resolvedArtwork = "";
  private String lastTitle = "";
  private String lastArtist = "";

  public RadioNameTag(RadioRuntime runtime, PlayerNameDisplayPosition position) {
    this.runtime = runtime;
    this.position = position;
  }

  @Override
  protected List<Component> buildComponents(EntitySnapshot snapshot) {
    this.visible = false;
    if (snapshot.isDiscrete()
        || this.runtime.configuration().playerCardPosition().get() != this.position) {
      return NO_COMPONENTS;
    }

    if (!this.resolveCardData(snapshot)) {
      return NO_COMPONENTS;
    }

    if (this.cover == null
        || !this.resolvedArtwork.equals(this.coverKey)
        || !this.resolvedTitle.equals(this.lastTitle)
        || !this.resolvedArtist.equals(this.lastArtist)) {
      this.rebuildCard();
    }
    this.visible = true;
    return CARD_COMPONENTS;
  }

  @Override
  protected float calculateWidth(Collection<Component> components) {
    return this.cardWidth;
  }

  @Override
  protected float calculateHeight(Collection<Component> components) {
    return this.cardHeight;
  }

  @Override
  public boolean isVisible() {
    return this.visible && super.isVisible();
  }

  @Override
  public float getScale() {
    return Math.max(0.5F, Math.min(1.5F, this.runtime.configuration().playerCardScale().get() / 100F));
  }

  @Override
  public void render(Stack stack, SubmissionCollector collector, EntitySnapshot snapshot) {
    if (!this.visible) {
      return;
    }

    int light = snapshot.lightCoords();
    collector.order(1).submitRectangle(
        stack, 0, 0, this.cardWidth, this.cardHeight, CARD_COLOR, light);

    float coverY = (this.cardHeight - COVER_SIZE) / 2F;
    collector.order(3).submitIcon(
        stack,
        this.cover,
        IconSubmission.DisplayMode.SEE_THROUGH,
        PADDING,
        coverY,
        COVER_SIZE,
        COVER_SIZE,
        WHITE);

    float lineHeight = this.fontRenderer.getLineHeight();
    float textHeight = lineHeight + (this.artistWidth > 0 ? lineHeight + 1 : 0);
    float textX = PADDING + COVER_SIZE + COVER_GAP;
    float textY = (this.cardHeight - textHeight) / 2F;
    this.submitText(stack, collector, snapshot, this.title, textX, textY);
    if (this.artistWidth > 0) {
      this.submitText(stack, collector, snapshot, this.artist, textX, textY + lineHeight + 1);
    }
  }

  @Override
  protected int getBackgroundColor(EntitySnapshot snapshot) {
    return 0;
  }

  private void rebuildCard() {
    this.coverKey = this.resolvedArtwork;
    this.cover = this.runtime.coverIcon(this.coverKey);
    this.lastTitle = this.resolvedTitle;
    this.lastArtist = this.resolvedArtist;
    this.title = Component.empty();
    this.artist = Component.empty();
    this.titleWidth = 0;
    this.artistWidth = 0;
    this.title = Component.text(this.shortenToWidth(this.resolvedTitle), TITLE_COLOR);
    this.titleWidth = this.fontRenderer.getWidth(this.title);
    if (!this.resolvedArtist.isBlank()) {
      this.artist = Component.text(this.shortenToWidth(this.resolvedArtist), ARTIST_COLOR);
      this.artistWidth = this.fontRenderer.getWidth(this.artist);
    }
    float lineHeight = this.fontRenderer.getLineHeight();
    float textHeight = lineHeight + (this.artistWidth > 0 ? lineHeight + 1 : 0);
    this.cardWidth = PADDING * 2 + COVER_SIZE + COVER_GAP
        + Math.max(this.titleWidth, this.artistWidth);
    this.cardHeight = PADDING * 2 + Math.max(COVER_SIZE, textHeight);
  }

  private boolean resolveCardData(EntitySnapshot snapshot) {
    ClientPlayer player = Laby.labyAPI().minecraft().getClientPlayer();
    if (player == null) {
      return false;
    }

    UUID renderedId = renderedPlayerId(snapshot);
    boolean localPlayer = renderedId != null && renderedId.equals(player.getUniqueId());
    if (localPlayer) {
      return this.resolveLocalCardData();
    }
    if (renderedId == null) {
      return false;
    }

    RemoteRadioPresence presence = this.runtime.remotePresence(renderedId);
    if (presence == null) {
      return false;
    }
    this.resolvedTitle = presence.title().isBlank() ? presence.station() : presence.title();
    this.resolvedArtist = presence.artist();
    this.resolvedArtwork = normalizeArtwork(presence.artworkUrl());
    return true;
  }

  private boolean resolveLocalCardData() {
    if (this.runtime == null || this.runtime.currentStation() == null) {
      return false;
    }
    PlayerState state = this.runtime.player().state();
    if (state != PlayerState.LOADING
        && state != PlayerState.PLAYING
        && state != PlayerState.PAUSED) {
      return false;
    }

    RadioStation station = this.runtime.currentStation();
    RadioMetadata metadata = station.metadata();
    this.resolvedArtwork = normalizeArtwork(
        metadata.coverUrl().isBlank() ? station.logoUrl() : metadata.coverUrl()
    );
    this.resolvedTitle = metadata.title().isBlank() ? station.name() : metadata.title();
    this.resolvedArtist = metadata.artist();
    return true;
  }

  private static UUID renderedPlayerId(EntitySnapshot snapshot) {
    if (snapshot.has(EntityExtraKeys.CUSTOM_AVATAR_DATA)) {
      CustomAvatarDataSnapshot avatar = snapshot.get(EntityExtraKeys.CUSTOM_AVATAR_DATA);
      if (avatar != null && avatar.playerInfo() != null && avatar.playerInfo().profile() != null) {
        return avatar.playerInfo().profile().getUniqueId();
      }
    }
    return null;
  }

  private String shortenToWidth(String value) {
    String normalized = value.trim();
    if (this.fontRenderer.getWidth(Component.text(normalized)) <= MAX_TEXT_WIDTH) {
      return normalized;
    }
    String shortened = normalized;
    while (shortened.length() > 1
        && this.fontRenderer.getWidth(Component.text(shortened + "…")) > MAX_TEXT_WIDTH) {
      shortened = shortened.substring(0, shortened.length() - 1);
    }
    return shortened.stripTrailing() + "…";
  }

  private static String normalizeArtwork(String artworkUrl) {
    return artworkUrl == null ? "" : artworkUrl.trim();
  }
}
