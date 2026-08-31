package net.dyhntastic.radio.core.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.core.RadioRuntime;
import net.dyhntastic.radio.core.audio.JLayerRadioPlayer;
import net.labymod.api.Textures;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.types.TitledActivity;
import net.labymod.api.client.gui.screen.widget.attributes.ObjectFitType;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget.ScrollMode;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.TilesGridWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.I18n;

@AutoActivity
public final class RadioActivity extends TitledActivity {

  private static final Icon BRAND_ICON = Icon.texture(
      ResourceLocation.create("dyhnunity-radio", "textures/icon.png")
  );
  private static final Icon FAVORITE_ICON = Icon.texture(
      ResourceLocation.create("dyhnunity-radio", "textures/favorite-star.png")
  );
  private static final Icon PLAY_ICON = Icon.texture(
      ResourceLocation.create("dyhnunity-radio", "textures/play.png")
  );
  private static final Icon PAUSE_ICON = Icon.texture(
      ResourceLocation.create("dyhnunity-radio", "textures/pause.png")
  );
  private static final Icon FALLBACK_COVER = Icon.texture(
      ResourceLocation.create("dyhnunity-radio", "textures/fallback-radio.png")
  );

  private final RadioRuntime runtime;
  private final AtomicLong uiGeneration = new AtomicLong();
  private final Map<String, RadioStation> previewCache = new HashMap<>();
  private RadioSource selectedSearchSource = RadioSource.LAUTFM;
  private List<RadioStation> visibleResults = List.of();
  private String browserMessage = "";
  private String searchQuery = "";
  private boolean initialDiscoveryRequested;
  private boolean favoritePreviewsRequested;
  private boolean favoritePreviewsLoading;
  private long nextFavoritePreviewRefreshAt;
  private ComponentWidget stationText;
  private ComponentWidget titleText;
  private ComponentWidget artistText;
  private ComponentWidget statusText;
  private ComponentWidget browserStatus;
  private IconWidget heroLogo;
  private ButtonWidget playPauseButton;
  private ButtonWidget currentFavoriteButton;
  private VerticalListWidget<Widget> favoritesHost;
  private VerticalListWidget<Widget> resultsHost;
  private VerticalListWidget<Widget> customStationsHost;
  private String currentFavoriteButtonState = "";
  private String playPauseButtonState = "";
  private String lastNotifiedPlayerError = "";
  private String lastHeroArtwork = "fallback";
  private TextFieldWidget searchField;
  private TextFieldWidget customName;
  private TextFieldWidget customStream;
  private VerticalListWidget<Widget> favoritesPage;
  private VerticalListWidget<Widget> discoveryPage;
  private VerticalListWidget<Widget> customPage;
  private ButtonWidget favoritesTab;
  private ButtonWidget discoveryTab;
  private ButtonWidget customTab;
  private Page selectedPage = Page.FAVORITES;
  private VerticalListWidget<Widget> activePage;
  private long pageTransitionStartedAt;
  private String customNameValue = "";
  private String customStreamValue = "";
  private String editingCustomId;
  private int stationColumns = 3;
  private RadioStation lastRenderedStation;
  private PlayerState lastRenderedState;
  private boolean lastRenderedMetadataChecked;
  private String lastRenderedError = "";

  public RadioActivity(RadioRuntime runtime) {
    super(Component.translatable("dyhnunity-radio.ui.title"));
    this.runtime = runtime;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addStyle("dyhnunity-radio", "radio.lss");
    this.stationColumns = stationColumnsFor(
        this.labyAPI.minecraft().minecraftWindow().getScaledWidth()
    );

    VerticalListWidget<Widget> content = new VerticalListWidget<>();
    content.addId("radio-content");
    content.spaceBetweenEntries().set(7.0F);

    if (this.runtime == null) {
      content.addChild(ComponentWidget.text(tr("startupPending")).addId("error"));
      this.document.addChild(content);
      return;
    }
    if (this.browserMessage.isBlank()) {
      this.browserMessage = tr("browser.activeCatalog", providerName(this.selectedSearchSource));
    }

    content.addChild(this.createHero());

    HorizontalListWidget controls = row("controls");
    this.playPauseButton = ButtonWidget.text(
        tr("controls.play"), PLAY_ICON, this::togglePlayback
    );
    this.playPauseButton.addId("primary-control");
    controls.addEntry(this.playPauseButton);
    controls.addEntry(ButtonWidget.text(
        tr("controls.reconnect"), Textures.SpriteCommon.REFRESH, this::reconnect
    ).addId("control-button"));
    this.currentFavoriteButton = ButtonWidget.text(
        tr("controls.favorite"), FAVORITE_ICON, this::toggleCurrentFavorite
    );
    this.currentFavoriteButtonState = "";
    this.currentFavoriteButton.addId("control-button");
    controls.addEntry(this.currentFavoriteButton);
    content.addChild(controls);

    SliderWidget volume = new SliderWidget(this.runtime::setVolume)
        .range(0, 100)
        .steps(1)
        .withFormatter(value -> Component.text(tr("controls.volume", Math.round(value))));
    volume.setValue(this.runtime.configuration().volume().get(), false);
    volume.addId("volume");
    content.addChild(volume);

    content.addChild(this.createPageNavigation());

    this.favoritesPage = page("favorites-page");
    this.favoritesPage.addChild(
        section(tr("sections.favorites.title"), tr("sections.favorites.subtitle"))
    );
    this.favoritesHost = stationHost("favorites-host");
    this.populateStationHost(
        this.favoritesHost,
        this.runtime.favorites(),
        tr("empty.favorites"),
        "favorites-grid",
        false
    );
    this.favoritesPage.addChild(this.favoritesHost);
    content.addChild(this.favoritesPage);

    this.discoveryPage = page("discovery-page");
    this.discoveryPage.addChild(section(
        tr("sections.discovery.title"),
        tr("sections.discovery.subtitle")
    ));
    HorizontalListWidget discoveryPrimary = row("discovery");
    discoveryPrimary.addEntry(ButtonWidget.text("laut.fm", Textures.SpriteCommon.BULLET_POINT, () ->
        this.loadDiscovery(RadioSource.LAUTFM)).addId("provider-lautfm"));
    discoveryPrimary.addEntry(ButtonWidget.text("Radio Browser", Textures.SpriteCommon.BULLET_POINT, () ->
        this.loadDiscovery(RadioSource.RADIO_BROWSER)).addId("provider-radio-browser"));
    this.discoveryPage.addChild(discoveryPrimary);
    HorizontalListWidget discoverySecondary = row("discovery");
    discoverySecondary.addEntry(ButtonWidget.text("I LOVE MUSIC", Textures.SpriteCommon.BULLET_POINT, () ->
        this.loadDiscovery(RadioSource.ILOVE_MUSIC)).addId("provider-ilove"));
    discoverySecondary.addEntry(ButtonWidget.text("RadioReg", Textures.SpriteCommon.BULLET_POINT, () ->
        this.loadDiscovery(RadioSource.RADIOREG)).addId("provider-radioreg"));
    this.discoveryPage.addChild(discoverySecondary);

    this.browserStatus = ComponentWidget.text(this.browserMessage)
        .addId("browser-status");
    this.discoveryPage.addChild(this.browserStatus);

    this.searchField = new TextFieldWidget()
        .placeholder(Component.text(tr("search.placeholder")));
    this.searchField.setText(this.searchQuery);
    this.searchField.updateListener(this::search);
    this.searchField.addId("search");
    this.discoveryPage.addChild(this.searchField);
    this.discoveryPage.addChild(
        section(tr("sections.preview.title"), tr("sections.preview.subtitle"))
    );
    this.resultsHost = stationHost("results-host");
    this.populateStationHost(
        this.resultsHost,
        this.visibleResults,
        this.browserMessage.isBlank() ? tr("empty.results") : this.browserMessage,
        "station-results",
        false
    );
    this.discoveryPage.addChild(this.resultsHost);
    content.addChild(this.discoveryPage);

    this.customPage = page("custom-page");
    this.customPage.addChild(section(tr("sections.custom.title"), tr("sections.custom.subtitle")));
    this.customName = field(tr("custom.namePlaceholder"), "custom-name");
    this.customStream = field(tr("custom.streamPlaceholder"), "custom-stream");
    this.customName.setText(this.customNameValue);
    this.customStream.setText(this.customStreamValue);
    this.customName.updateListener(value -> this.customNameValue = value);
    this.customStream.updateListener(value -> this.customStreamValue = value);
    this.customPage.addChild(this.customName);
    this.customPage.addChild(this.customStream);
    this.customPage.addChild(ButtonWidget.text(
        tr("custom.save"), Textures.SpriteCommon.ADD, this::saveCustom
    ).addId("save-custom"));

    this.customStationsHost = stationHost("custom-stations-host");
    this.populateStationHost(
        this.customStationsHost,
        this.runtime.customStations(),
        tr("empty.custom"),
        "custom-grid",
        false
    );
    this.customPage.addChild(this.customStationsHost);
    content.addChild(this.customPage);

    this.switchPage(this.selectedPage, false);

    content.addChild(ButtonWidget.text(tr("openWebsite"), () ->
        this.labyAPI.minecraft().chatExecutor().openUrl("https://dyhnunityfm.de")
    ).addId("branding"));

    ScrollWidget scroll = new ScrollWidget(content);
    scroll.addId("radio-scroll");
    this.document.addChild(scroll);
    this.updateNowPlaying();
    if (!this.initialDiscoveryRequested) {
      this.initialDiscoveryRequested = true;
      this.addInitializeRunnable(() -> this.loadDiscovery(RadioSource.LAUTFM));
    }
    if (!this.favoritePreviewsRequested) {
      this.favoritePreviewsRequested = true;
      this.addInitializeRunnable(this::loadFavoritePreviews);
    }
  }

  private DivWidget createHero() {
    DivWidget hero = new DivWidget();
    hero.addId("radio-hero");

    this.heroLogo = new IconWidget(FALLBACK_COVER);
    this.heroLogo.objectFit().set(ObjectFitType.CONTAIN);
    this.heroLogo.addId("brand-logo");
    hero.addChild(this.heroLogo);

    hero.addChild(ComponentWidget.text(
        tr("hero.info")
    ).addId("brand-info"));
    this.titleText = scrollingText(tr("hero.readyTitle"), "song-title", 14.0F);
    this.artistText = scrollingText(tr("hero.readyArtist"), "song-artist", 14.0F);
    this.stationText = scrollingText(tr("hero.noStation"), "station-name", 13.0F);
    this.statusText = ComponentWidget.text(tr("state.stopped")).addId("player-status");
    hero.addChild(this.titleText);
    hero.addChild(this.artistText);
    hero.addChild(this.stationText);
    hero.addChild(this.statusText);

    return hero;
  }

  private HorizontalListWidget createPageNavigation() {
    HorizontalListWidget navigation = row("page-navigation");
    this.favoritesTab = ButtonWidget.text(
        tr("navigation.favorites"), FAVORITE_ICON, () -> this.switchPage(Page.FAVORITES, true)
    );
    this.discoveryTab = ButtonWidget.text(
        tr("navigation.discovery"), Textures.SpriteCommon.BULLET_POINT,
        () -> this.switchPage(Page.DISCOVERY, true)
    );
    this.customTab = ButtonWidget.text(
        tr("navigation.custom"), Textures.SpriteCommon.ADD,
        () -> this.switchPage(Page.CUSTOM, true)
    );
    this.favoritesTab.addId("page-tab");
    this.discoveryTab.addId("page-tab");
    this.customTab.addId("page-tab");
    navigation.addEntry(this.favoritesTab);
    navigation.addEntry(this.discoveryTab);
    navigation.addEntry(this.customTab);
    return navigation;
  }

  private void switchPage(Page page, boolean animate) {
    this.selectedPage = page;
    if (this.favoritesPage == null || this.discoveryPage == null || this.customPage == null) {
      return;
    }
    this.favoritesPage.setVisible(page == Page.FAVORITES);
    this.discoveryPage.setVisible(page == Page.DISCOVERY);
    this.customPage.setVisible(page == Page.CUSTOM);
    this.favoritesTab.setSelected(page == Page.FAVORITES);
    this.discoveryTab.setSelected(page == Page.DISCOVERY);
    this.customTab.setSelected(page == Page.CUSTOM);
    this.activePage = switch (page) {
      case FAVORITES -> this.favoritesPage;
      case DISCOVERY -> this.discoveryPage;
      case CUSTOM -> this.customPage;
    };
    if (animate) {
      this.activePage.opacity().set(0.2F);
      this.pageTransitionStartedAt = System.currentTimeMillis();
    } else {
      this.activePage.opacity().set(1.0F);
      this.pageTransitionStartedAt = 0L;
    }
  }

  private void updatePageTransition() {
    if (this.pageTransitionStartedAt == 0L || this.activePage == null) {
      return;
    }
    float progress = Math.min(
        1.0F,
        (System.currentTimeMillis() - this.pageTransitionStartedAt) / 180.0F
    );
    float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
    this.activePage.opacity().set(0.2F + 0.8F * eased);
    if (progress >= 1.0F) {
      this.pageTransitionStartedAt = 0L;
    }
  }

  @Override
  public void tick() {
    super.tick();
    this.updateNowPlaying();
    this.updatePageTransition();
    if (System.currentTimeMillis() >= this.nextFavoritePreviewRefreshAt) {
      this.loadFavoritePreviews();
    }
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    int columns = stationColumnsFor(this.labyAPI.minecraft().minecraftWindow().getScaledWidth());
    if (columns != this.stationColumns) {
      this.stationColumns = columns;
      this.updateStationGridColumns();
    }
  }

  @Override
  public void onCloseScreen() {
    this.uiGeneration.incrementAndGet();
    super.onCloseScreen();
  }

  private void search(String query) {
    this.searchQuery = query == null ? "" : query;
    if (query == null || query.isBlank()) {
      this.loadDiscovery(this.selectedSearchSource);
      return;
    }
    long generation = this.uiGeneration.incrementAndGet();
    RadioSource source = this.selectedSearchSource;
    this.setBrowserStatus(tr("browser.searching", providerName(source)));
    this.runtime.search(source, query, 0, 24).whenComplete((page, error) ->
        this.onRenderThread(generation, () -> {
          if (error != null) {
            this.showResults(
                generation,
                List.of(),
                tr("browser.searchUnavailable", providerName(source))
            );
          } else {
            this.showResults(
                generation,
                page.stations(),
                page.stations().isEmpty()
                    ? tr("browser.noMatches", providerName(source))
                    : ""
            );
          }
        })
    );
  }

  private void loadDiscovery(RadioSource source) {
    this.selectedSearchSource = source;
    if (this.searchField != null && !this.searchField.getText().isBlank()) {
      this.search(this.searchField.getText());
      return;
    }
    long generation = this.uiGeneration.incrementAndGet();
    this.setBrowserStatus(tr("browser.loading", providerName(source)));
    this.runtime.discover(source, 0, 24).whenComplete((page, error) ->
        this.onRenderThread(generation, () -> {
          if (error != null) {
            this.showResults(
                generation,
                List.of(),
                tr("browser.unavailable", providerName(source))
            );
          } else {
            this.showResults(generation, page.stations(), "");
          }
        })
    );
  }

  private void showResults(long generation, List<RadioStation> stations, String emptyMessage) {
    this.visibleResults = List.copyOf(stations);
    this.browserMessage = stations.isEmpty()
        ? emptyMessage
        : tr("browser.loaded", stations.size(), providerName(this.selectedSearchSource));
    this.setBrowserStatus(this.browserMessage);
    this.refreshStationHost(
        this.resultsHost,
        this.visibleResults,
        this.browserMessage.isBlank() ? tr("empty.noStations") : this.browserMessage,
        "station-results"
    );
    if (!stations.isEmpty()) {
      this.loadResultPreviews(generation, stations);
    }
  }

  private void addStations(TilesGridWidget<Widget> target, List<RadioStation> stations) {
    for (RadioStation station : stations) {
      target.addTile(this.stationCard(this.stationForDisplay(station)));
    }
  }

  private void refreshAllStationHosts() {
    this.refreshStationHost(
        this.favoritesHost,
        this.runtime.favorites(),
        tr("empty.favorites"),
        "favorites-grid"
    );
    this.refreshStationHost(
        this.resultsHost,
        this.visibleResults,
        this.browserMessage.isBlank() ? tr("empty.results") : this.browserMessage,
        "station-results"
    );
    this.refreshStationHost(
        this.customStationsHost,
        this.runtime.customStations(),
        tr("empty.custom"),
        "custom-grid"
    );
  }

  private void refreshStationHost(
      VerticalListWidget<Widget> host,
      List<RadioStation> stations,
      String emptyMessage,
      String gridId
  ) {
    if (host == null) {
      return;
    }
    for (Widget child : List.copyOf(host.getChildren())) {
      host.removeChildImmediately(child);
    }
    this.populateStationHost(host, stations, emptyMessage, gridId, true);
  }

  private void populateStationHost(
      VerticalListWidget<Widget> host,
      List<RadioStation> stations,
      String emptyMessage,
      String gridId,
      boolean initialized
  ) {
    Widget child;
    if (stations.isEmpty()) {
      child = ComponentWidget.text(emptyMessage).addId("empty-stations");
    } else {
      TilesGridWidget<Widget> grid = stationGrid(gridId);
      this.addStations(grid, stations);
      child = grid;
    }
    if (initialized) {
      host.addChildInitialized(child);
    } else {
      host.addChild(child);
    }
    if (child instanceof TilesGridWidget<?> grid) {
      grid.tilesPerLine().set(this.stationColumns);
      if (initialized) {
        grid.updateTiles();
      }
    }
  }

  private void updateStationGridColumns() {
    this.updateStationGridColumns(this.favoritesHost);
    this.updateStationGridColumns(this.resultsHost);
    this.updateStationGridColumns(this.customStationsHost);
  }

  private void updateStationGridColumns(VerticalListWidget<Widget> host) {
    if (host == null) {
      return;
    }
    for (Widget child : host.getChildren()) {
      if (child instanceof TilesGridWidget<?> grid) {
        grid.tilesPerLine().set(this.stationColumns);
        grid.updateTiles();
      }
    }
  }

  private DivWidget stationCard(RadioStation station) {
    DivWidget card = new DivWidget();
    card.addId("station-card");
    card.addId(switch (station.source()) {
      case LAUTFM -> "station-source-lautfm";
      case RADIO_BROWSER -> "station-source-radio-browser";
      case ILOVE_MUSIC -> "station-source-ilove";
      case RADIOREG -> "station-source-radioreg";
      case CUSTOM -> "station-source-custom";
    });
    if (station.source() == RadioSource.CUSTOM) {
      card.addId("custom-station-card");
    }
    card.setPressable(() -> this.playStation(station));

    Icon stationIcon = station.logoUrl().isBlank()
        ? FALLBACK_COVER
        : this.runtime.coverIcon(station.logoUrl());
    IconWidget logo = new IconWidget(stationIcon);
    logo.objectFit().set(ObjectFitType.CONTAIN);
    logo.addId("station-logo");
    card.addChild(logo);

    IconWidget play = new IconWidget(PLAY_ICON);
    play.objectFit().set(ObjectFitType.CONTAIN);
    play.addId("station-card-play");
    card.addChild(play);

    card.addChild(ComponentWidget.text(station.name()).addId("station-play"));

    card.addChild(scrollingText(this.metadataLine(station), "station-subtitle", 12.0F));

    boolean favoriteSelected = this.runtime.isFavorite(station.id());
    ButtonWidget favorite = ButtonWidget.icon(FAVORITE_ICON);
    favorite.setSelected(favoriteSelected);
    favorite.setPressable(() -> {
      this.runtime.toggleFavorite(station);
      boolean selected = this.runtime.isFavorite(station.id());
      this.notifyUser(
          selected ? tr("notifications.favoriteAdded.title") : tr("notifications.favoriteRemoved.title"),
          selected
              ? tr("notifications.favoriteAdded.text", station.name())
              : tr("notifications.favoriteRemoved.text", station.name())
      );
      this.updateCurrentFavoriteButton();
      this.refreshAllStationHosts();
    });
    favorite.addId("station-favorite");
    card.addChild(favorite);

    if (station.source() == RadioSource.CUSTOM) {
      ButtonWidget edit = ButtonWidget.icon(Textures.SpriteCommon.EDIT, () -> this.editCustom(station));
      edit.addId("station-edit");
      card.addChild(edit);
      ButtonWidget delete = ButtonWidget.icon(Textures.SpriteCommon.TRASH, () -> {
        this.runtime.deleteCustom(station.id());
        this.previewCache.remove(station.id());
        this.notifyUser(
            tr("notifications.stationDeleted.title"),
            tr("notifications.stationDeleted.text", station.name())
        );
        this.refreshAllStationHosts();
      });
      delete.addId("station-delete");
      card.addChild(delete);
    }
    return card;
  }

  private void saveCustom() {
    try {
      RadioStation station = this.runtime.saveCustom(
          this.editingCustomId,
          this.customName.getText(),
          this.customStream.getText()
      );
      this.editingCustomId = null;
      this.customNameValue = "";
      this.customStreamValue = "";
      this.previewCache.put(station.id(), station);
      this.notifyUser(
          tr("notifications.stationSaved.title"),
          tr("notifications.stationSaved.text", station.name())
      );
      this.customName.setText("");
      this.customStream.setText("");
      this.refreshAllStationHosts();
    } catch (IllegalArgumentException exception) {
      this.customNameValue = this.customName.getText();
      this.customStreamValue = this.customStream.getText();
      this.notifyUser(
          tr("notifications.stationSaveFailed.title"),
          validationMessage(exception)
      );
    }
  }

  private void editCustom(RadioStation station) {
    this.editingCustomId = station.id();
    this.customNameValue = station.name();
    this.customStreamValue = station.streamUrl();
    this.notifyUser(
        tr("notifications.stationEdit.title"),
        tr("notifications.stationEdit.text", station.name())
    );
    this.customName.setText(this.customNameValue);
    this.customStream.setText(this.customStreamValue);
  }

  private void toggleCurrentFavorite() {
    RadioStation station = this.runtime.currentStation();
    if (station == null) {
      this.notifyNoStation();
      return;
    }
    this.runtime.toggleFavorite(station);
    boolean selected = this.runtime.isFavorite(station.id());
    this.notifyUser(
        selected ? tr("notifications.favoriteAdded.title") : tr("notifications.favoriteRemoved.title"),
        selected
            ? tr("notifications.favoriteAdded.text", station.name())
            : tr("notifications.favoriteRemoved.text", station.name())
    );
    this.updateCurrentFavoriteButton();
    this.refreshAllStationHosts();
  }

  private void togglePlayback() {
    if (this.runtime.currentStation() == null) {
      this.notifyNoStation();
      return;
    }
    PlayerState previous = this.runtime.player().state();
    this.runtime.togglePause();
    RadioStation station = this.runtime.currentStation();
    if (previous == PlayerState.PAUSED) {
      this.notifyUser(
          tr("notifications.playbackResumed.title"),
          tr("notifications.playbackResumed.text", station.name())
      );
    } else if (previous == PlayerState.PLAYING || previous == PlayerState.LOADING) {
      this.notifyUser(
          tr("notifications.playbackPaused.title"),
          tr("notifications.playbackPaused.text", station.name())
      );
    } else {
      if (this.runtime.player().state() == PlayerState.ERROR) {
        this.notifyCurrentPlaybackError();
      } else {
        this.notifyUser(
            tr("notifications.stationStarting.title"),
            tr("notifications.stationStarting.text", station.name())
        );
      }
    }
  }

  private void reconnect() {
    RadioStation station = this.runtime.currentStation();
    if (station == null) {
      this.notifyNoStation();
      return;
    }
    this.runtime.reconnect();
    if (this.runtime.player().state() == PlayerState.ERROR) {
      this.notifyCurrentPlaybackError();
    } else {
      this.notifyUser(
          tr("notifications.reconnecting.title"),
          tr("notifications.reconnecting.text", station.name())
      );
    }
  }

  private void updateNowPlaying() {
    if (this.runtime == null || this.stationText == null) {
      return;
    }
    RadioStation station = this.runtime.currentStation();
    RadioMetadata metadata = station == null ? RadioMetadata.EMPTY : station.metadata();
    PlayerState state = this.runtime.player().state();
    boolean metadataChecked = station != null && this.runtime.metadataChecked(station.id());
    String playerError = state == PlayerState.ERROR ? this.runtime.player().errorMessage() : "";
    boolean contentChanged = station != this.lastRenderedStation
        || state != this.lastRenderedState
        || metadataChecked != this.lastRenderedMetadataChecked
        || !playerError.equals(this.lastRenderedError);
    if (contentChanged) {
      this.stationText.setText(station == null ? tr("hero.noStation") : station.name());
      if (station == null) {
        this.titleText.setText(tr("hero.readyTitle"));
        this.artistText.setText(tr("hero.readyArtist"));
      } else {
        this.titleText.setText(metadata.title().isBlank()
            ? tr(metadataChecked ? "metadata.unavailableTitle" : "metadata.loadingTitle")
            : metadata.title());
        this.artistText.setText(metadata.artist().isBlank()
            ? tr(metadataChecked ? "metadata.unknownArtist" : "metadata.checking")
            : metadata.artist());
      }
      if (state == PlayerState.ERROR) {
        this.statusText.setText(tr(
            "state.errorWithMessage",
            this.localizedPlayerError(playerError)
        ));
        this.notifyCurrentPlaybackError();
      } else {
        this.lastNotifiedPlayerError = "";
        this.statusText.setText(switch (state) {
          case LOADING -> tr("state.loading");
          case PLAYING -> tr("state.playing");
          case PAUSED -> tr("state.paused");
          case STOPPED -> tr("state.stopped");
          case ERROR -> tr("state.error");
        });
      }
      this.lastRenderedStation = station;
      this.lastRenderedState = state;
      this.lastRenderedMetadataChecked = metadataChecked;
      this.lastRenderedError = playerError;
    }
    this.updatePlaybackButton(station, state);
    this.updateCurrentFavoriteButton();
    String artworkUrl = "";
    if (station != null) {
      if (!metadata.coverUrl().isBlank()) {
        artworkUrl = metadata.coverUrl();
      } else if (!station.logoUrl().isBlank()) {
        artworkUrl = station.logoUrl();
      }
    }
    String artworkKey = artworkUrl.isBlank() ? "fallback" : artworkUrl;
    if (!artworkKey.equals(this.lastHeroArtwork)) {
      this.lastHeroArtwork = artworkKey;
      this.heroLogo.icon().set(
          artworkUrl.isBlank() ? FALLBACK_COVER : this.runtime.coverIcon(artworkUrl)
      );
    }
  }

  private void updatePlaybackButton(RadioStation station, PlayerState state) {
    if (this.playPauseButton == null) {
      return;
    }
    String nextState = station == null ? "disabled" : state.name();
    if (nextState.equals(this.playPauseButtonState)) {
      return;
    }
    this.playPauseButtonState = nextState;
    boolean active = state == PlayerState.PLAYING || state == PlayerState.LOADING;
    boolean paused = state == PlayerState.PAUSED;
    this.playPauseButton.updateComponent(Component.text(
        tr(active ? "controls.pause" : paused ? "controls.resume" : "controls.play")
    ));
    this.playPauseButton.updateIcon(active ? PAUSE_ICON : PLAY_ICON);
    this.playPauseButton.setSelected(active);
    this.playPauseButton.setEnabled(station != null);
  }

  private void onRenderThread(long generation, Runnable action) {
    this.labyAPI.minecraft().executeOnRenderThread(() -> {
      if (this.isOpen() && this.uiGeneration.get() == generation) {
        action.run();
      }
    });
  }

  private void onRenderThread(Runnable action) {
    this.labyAPI.minecraft().executeOnRenderThread(() -> {
      if (this.isOpen()) {
        action.run();
      }
    });
  }

  private void setBrowserStatus(String status) {
    this.browserMessage = status == null ? "" : status;
    if (this.browserStatus != null) {
      this.browserStatus.setText(this.browserMessage);
    }
  }

  private void updateCurrentFavoriteButton() {
    if (this.currentFavoriteButton == null) {
      return;
    }
    RadioStation station = this.runtime.currentStation();
    boolean selected = station != null && this.runtime.isFavorite(station.id());
    String state = station == null ? "none" : station.id() + ':' + selected;
    if (state.equals(this.currentFavoriteButtonState)) {
      return;
    }
    this.currentFavoriteButtonState = state;
    this.currentFavoriteButton.updateComponent(Component.text(
        selected ? tr("controls.favoriteSaved") : tr("controls.favorite")
    ));
    this.currentFavoriteButton.updateIcon(
        FAVORITE_ICON
    );
    this.currentFavoriteButton.setSelected(selected);
  }

  private void loadFavoritePreviews() {
    if (this.favoritePreviewsLoading) {
      return;
    }
    this.nextFavoritePreviewRefreshAt = System.currentTimeMillis() + 15_000L;
    List<RadioStation> favorites = this.runtime.favorites();
    if (favorites.isEmpty()) {
      return;
    }
    this.favoritePreviewsLoading = true;
    AtomicInteger remaining = new AtomicInteger(favorites.size());
    for (RadioStation favorite : favorites) {
      this.runtime.refreshPreview(favorite).whenComplete((station, error) -> {
        boolean last = remaining.decrementAndGet() == 0;
        this.onRenderThread(() -> {
          if (error == null && station != null && this.cachePreviews(List.of(station))) {
            this.refreshStationHost(
                this.favoritesHost,
                this.runtime.favorites(),
                tr("empty.favorites"),
                "favorites-grid"
            );
          }
          if (last) {
            this.favoritePreviewsLoading = false;
          }
        });
      });
    }
  }

  private void loadResultPreviews(long generation, List<RadioStation> stations) {
    this.runtime.loadPreviews(stations).whenComplete((previews, error) ->
        this.onRenderThread(generation, () -> {
          if (error != null) {
            return;
          }
          this.cachePreviews(previews);
          this.visibleResults = previews;
          this.refreshStationHost(
              this.resultsHost,
              this.visibleResults,
              this.browserMessage.isBlank() ? tr("empty.results") : this.browserMessage,
              "station-results"
          );
        })
    );
  }

  private boolean cachePreviews(List<RadioStation> stations) {
    boolean changed = false;
    for (RadioStation station : stations) {
      RadioStation previous = this.previewCache.put(station.id(), station);
      changed |= !station.equals(previous);
    }
    return changed;
  }

  private RadioStation stationForDisplay(RadioStation station) {
    RadioStation current = this.runtime.currentStation();
    if (current != null && current.id().equals(station.id())) {
      return current;
    }
    return this.previewCache.getOrDefault(station.id(), station);
  }

  private void notifyUser(String title, String text) {
    Notification.builder()
        .title(Component.text(title))
        .text(Component.text(text == null ? "" : text))
        .icon(BRAND_ICON)
        .duration(10000L)
        .type(Notification.Type.SYSTEM)
        .buildAndPush();
  }

  private void notifyNoStation() {
    this.notifyUser(
        tr("notifications.noStation.title"),
        tr("notifications.noStation.text")
    );
  }

  private void playStation(RadioStation station) {
    this.runtime.play(station);
    if (this.runtime.player().state() == PlayerState.ERROR) {
      this.notifyCurrentPlaybackError();
      return;
    }
    this.notifyUser(
        tr("notifications.stationStarting.title"),
        tr("notifications.stationStarting.text", station.name())
    );
  }

  private void notifyCurrentPlaybackError() {
    String error = this.runtime.player().errorMessage();
    if (error == null || error.isBlank() || error.equals(this.lastNotifiedPlayerError)) {
      return;
    }
    this.lastNotifiedPlayerError = error;
    this.notifyUser(
        tr("notifications.playbackError.title"),
        tr("notifications.playbackError.text", this.localizedPlayerError(error))
    );
  }

  private String localizedPlayerError(String error) {
    String value = error == null ? "" : error;
    if (value.startsWith(JLayerRadioPlayer.UNSUPPORTED_FORMAT_ERROR)) {
      String format = value.substring(JLayerRadioPlayer.UNSUPPORTED_FORMAT_ERROR.length());
      return tr("errors.unsupportedFormat", format);
    }
    if (JLayerRadioPlayer.INVALID_STREAM_ERROR.equals(value)) {
      return tr("errors.invalidStreamUrl");
    }
    if (JLayerRadioPlayer.PLAYBACK_FAILED_ERROR.equals(value)) {
      return tr("errors.playbackFailed");
    }
    return tr("errors.unknown");
  }

  private static DivWidget section(String title, String subtitle) {
    DivWidget section = new DivWidget();
    section.addId("section-heading");
    section.addChild(ComponentWidget.text(title).addId("section-title"));
    section.addChild(ComponentWidget.text(subtitle).addId("section-subtitle"));
    return section;
  }

  private static TextFieldWidget field(String placeholder, String id) {
    TextFieldWidget field = new TextFieldWidget().placeholder(Component.text(placeholder));
    field.addId(id);
    return field;
  }

  private static ComponentWidget scrollingText(String text, String id, float speed) {
    ComponentWidget widget = ComponentWidget.text(text).addId(id);
    widget.scrollText().set(true);
    widget.scrollMode().set(ScrollMode.LOOP);
    widget.scrollSpeed().set(speed);
    widget.scrollGap().set(24.0F);
    return widget;
  }

  private static HorizontalListWidget row(String id) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId(id);
    row.spaceBetweenEntries().set(5);
    return row;
  }

  private static VerticalListWidget<Widget> stationHost(String id) {
    VerticalListWidget<Widget> host = new VerticalListWidget<>();
    host.addId("station-host");
    host.addId(id);
    host.spaceBetweenEntries().set(0.0F);
    return host;
  }

  private static VerticalListWidget<Widget> page(String id) {
    VerticalListWidget<Widget> page = new VerticalListWidget<>();
    page.addId("radio-page");
    page.addId(id);
    page.spaceBetweenEntries().set(6.0F);
    return page;
  }

  private TilesGridWidget<Widget> stationGrid(String id) {
    TilesGridWidget<Widget> grid = new TilesGridWidget<>();
    grid.addId("station-grid");
    grid.addId(id);
    grid.spaceBetweenEntries().set(5.0F);
    grid.tilesPerLine().set(this.stationColumns);
    grid.tileHeight().set(70.0F);
    return grid;
  }

  private String metadataLine(RadioStation station) {
    String unsupportedFormat = this.runtime.knownUnsupportedFormat(station);
    if (!unsupportedFormat.isBlank()) {
      return tr("metadata.unsupportedFormat", unsupportedFormat);
    }
    RadioMetadata metadata = station.metadata();
    String artist = metadata.artist().isBlank() ? tr("metadata.unknownArtist") : metadata.artist();
    String title = metadata.title().isBlank()
        ? tr(this.runtime.metadataChecked(station.id())
            ? "metadata.unavailableTitle"
            : "metadata.loadingTitle")
        : metadata.title();
    return artist + "  ·  " + title;
  }

  private static String providerName(RadioSource source) {
    return switch (source) {
      case LAUTFM -> "laut.fm";
      case RADIO_BROWSER -> "Radio Browser";
      case ILOVE_MUSIC -> "I LOVE MUSIC";
      case RADIOREG -> "RadioReg";
      case CUSTOM -> tr("provider.custom");
    };
  }

  private static String validationMessage(IllegalArgumentException exception) {
    String message = String.valueOf(exception.getMessage());
    if (message.startsWith("Unsupported stream format: ")) {
      return tr("validation.unsupportedStreamFormat", message.substring(27));
    }
    return switch (message) {
      case "Station name is required" -> tr("validation.nameRequired");
      case "Station name is too long" -> tr("validation.nameTooLong");
      case "Stream URL is too long" -> tr("validation.streamUrlTooLong");
      case "Stream URL must be HTTP or HTTPS" -> tr("validation.invalidStreamUrl");
      default -> exception.getMessage() == null
          ? tr("validation.unknown")
          : exception.getMessage();
    };
  }

  private static String tr(String key, Object... arguments) {
    return I18n.translate("dyhnunity-radio.ui." + key, arguments);
  }

  private static int stationColumnsFor(int scaledWidth) {
    if (scaledWidth >= 760) {
      return 3;
    }
    return scaledWidth >= 420 ? 2 : 1;
  }

  private enum Page {
    FAVORITES,
    DISCOVERY,
    CUSTOM
  }
}
