package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.label
import dev.jaysce.jukeboxkt.viewmodel.ContentViewModel
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.AppKit.*
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSEdgeInsetsMake
import platform.Foundation.NSMakeRect
import platform.Foundation.NSSelectorFromString

class ContentPopoverView(
  private val viewModel: ContentViewModel,
) : NSView(NSMakeRect(0.0, 0.0, 272.0, 350.0)) {

  private val albumArtContainer = NSView()
  private val albumArtImageView = NSImageView()
  private val titleLabel = label()
  private val artistLabel = label()
  private val timeLabel = label()
  private val notRunningLabel = label()

  private val favoriteButton = NSButton()
  private val prevButton = NSButton()
  private val playPauseButton = NSButton()
  private val nextButton = NSButton()
  private val controlsStack = NSStackView()
  private val controlsGlass = NSGlassEffectView()

  private val backgroundImageView = NSImageView()
  private val blurView = NSVisualEffectView()

  init {
    wantsLayer = true
    setupSubviews()
    setupConstraints()
    viewModel.addListener { updateUI() }
    updateUI()
  }

  override fun layout() {
    super.layout()
    // Background bleed: frame-based to avoid inflating fittingSize
    val bleed = 12.0
    bounds.useContents {
      val bleedFrame = NSMakeRect(-bleed, -bleed, size.width + 2 * bleed, size.height + 2 * bleed)
      backgroundImageView.frame = bleedFrame
      blurView.frame = bleedFrame
    }
    // Controls pill: 실제 높이 기반 균일한 cornerRadius
    controlsGlass.bounds.useContents {
      controlsGlass.cornerRadius = size.height / 2
    }
  }

  private fun setupSubviews() {
    // Background (frame-based, positioned in layout() with bleed)
    backgroundImageView.apply {
      imageScaling = NSImageScaleProportionallyUpOrDown
    }
    addSubview(backgroundImageView)

    blurView.apply {
      material = NSVisualEffectMaterialPopover
      blendingMode = NSVisualEffectBlendingMode.NSVisualEffectBlendingModeWithinWindow
    }
    addSubview(blurView)

    // "Not running" message
    notRunningLabel.apply {
      translatesAutoresizingMaskIntoConstraints = false
      font = NSFont.boldSystemFontOfSize(24.0)
      textColor = NSColor.secondaryLabelColor
      alignment = NSTextAlignmentCenter
      maximumNumberOfLines = 2
    }
    addSubview(notRunningLabel)

    // Album art
    albumArtContainer.apply {
      translatesAutoresizingMaskIntoConstraints = false
      wantsLayer = true
      layer?.shadowColor = NSColor.blackColor.colorWithAlphaComponent(0.2).CGColor
      layer?.shadowOffset = CGSizeMake(0.0, -2.0)
      layer?.shadowRadius = 4.0
      layer?.shadowOpacity = 1.0f
    }
    addSubview(albumArtContainer)

    albumArtImageView.apply {
      translatesAutoresizingMaskIntoConstraints = false
      imageScaling = NSImageScaleProportionallyUpOrDown
      wantsLayer = true
      layer?.cornerRadius = 8.0
      layer?.masksToBounds = true
    }
    albumArtContainer.addSubview(albumArtImageView)

    // Controls overlay (pill-shaped blur on album art)
    setupControls()

    // Track info
    titleLabel.apply {
      translatesAutoresizingMaskIntoConstraints = false
      font = NSFont.boldSystemFontOfSize(15.0)
      textColor = NSColor.labelColor.colorWithAlphaComponent(0.8)
      alignment = NSTextAlignmentCenter
      lineBreakMode = NSLineBreakByTruncatingTail
    }
    addSubview(titleLabel)

    artistLabel.apply {
      translatesAutoresizingMaskIntoConstraints = false
      font = NSFont.systemFontOfSize(13.0, weight = NSFontWeightMedium)
      textColor = NSColor.labelColor.colorWithAlphaComponent(0.6)
      alignment = NSTextAlignmentCenter
      lineBreakMode = NSLineBreakByTruncatingTail
    }
    addSubview(artistLabel)

    timeLabel.apply {
      translatesAutoresizingMaskIntoConstraints = false
      font = NSFont.systemFontOfSize(11.0)
      textColor = NSColor.labelColor.colorWithAlphaComponent(0.6)
      alignment = NSTextAlignmentCenter
    }
    addSubview(timeLabel)
  }

  private fun setupControls() {
    fun initButton(button: NSButton, symbolName: String, action: String) {
      button.bezelStyle = NSBezelStyleAccessoryBarAction
      button.setBordered(false)
      button.image = NSImage.imageWithSystemSymbolName(symbolName, accessibilityDescription = null)
      button.target = this@ContentPopoverView
      button.action = NSSelectorFromString(action)
    }

    initButton(favoriteButton, "heart", "favoriteClicked:")
    initButton(prevButton, "backward.end.fill", "prevClicked:")
    initButton(playPauseButton, "play.fill", "playPauseClicked:")
    initButton(nextButton, "forward.end.fill", "nextClicked:")

    controlsGlass.apply {
      translatesAutoresizingMaskIntoConstraints = false
    }
    addSubview(controlsGlass)

    controlsStack.apply {
      translatesAutoresizingMaskIntoConstraints = false
      addArrangedSubview(favoriteButton)
      addArrangedSubview(prevButton)
      addArrangedSubview(playPauseButton)
      addArrangedSubview(nextButton)
      orientation = NSUserInterfaceLayoutOrientationHorizontal
      spacing = 6.0
    }

    // NSGlassEffectView.contentView는 edgeInsets를 무시하므로 wrapper로 패딩 적용
    val wrapper = NSView()
    wrapper.addSubview(controlsStack)
    NSLayoutConstraint.activateConstraints(listOf(
      controlsStack.topAnchor.constraintEqualToAnchor(wrapper.topAnchor, constant = 10.0),
      controlsStack.bottomAnchor.constraintEqualToAnchor(wrapper.bottomAnchor, constant = -10.0),
      controlsStack.leadingAnchor.constraintEqualToAnchor(wrapper.leadingAnchor, constant = 14.0),
      controlsStack.trailingAnchor.constraintEqualToAnchor(wrapper.trailingAnchor, constant = -14.0),
    ))
    controlsGlass.contentView = wrapper
  }

  private fun setupConstraints() {
    val padding = 16.0

    NSLayoutConstraint.activateConstraints(listOf(
      // Not running label (centered)
      notRunningLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor),
      notRunningLabel.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
      notRunningLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = padding),
      notRunningLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -padding),

      // Album art container (leading+trailing+width defines parent width = 272)
      albumArtContainer.topAnchor.constraintEqualToAnchor(topAnchor, constant = padding),
      albumArtContainer.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = padding),
      albumArtContainer.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -padding),
      albumArtContainer.widthAnchor.constraintEqualToConstant(240.0),
      albumArtContainer.heightAnchor.constraintEqualToConstant(240.0),

      // Album art image (fill container)
      albumArtImageView.topAnchor.constraintEqualToAnchor(albumArtContainer.topAnchor),
      albumArtImageView.bottomAnchor.constraintEqualToAnchor(albumArtContainer.bottomAnchor),
      albumArtImageView.leadingAnchor.constraintEqualToAnchor(albumArtContainer.leadingAnchor),
      albumArtImageView.trailingAnchor.constraintEqualToAnchor(albumArtContainer.trailingAnchor),

      // Controls glass pill (contentView + edgeInsets handles internal padding)
      controlsGlass.centerXAnchor.constraintEqualToAnchor(albumArtContainer.centerXAnchor),
      controlsGlass.bottomAnchor.constraintEqualToAnchor(albumArtContainer.bottomAnchor, constant = -8.0),

      // Title (below album art)
      titleLabel.topAnchor.constraintEqualToAnchor(albumArtContainer.bottomAnchor, constant = 20.0),
      titleLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = 28.0),
      titleLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -28.0),

      // Artist (below title)
      artistLabel.topAnchor.constraintEqualToAnchor(titleLabel.bottomAnchor, constant = 2.0),
      artistLabel.leadingAnchor.constraintEqualToAnchor(titleLabel.leadingAnchor),
      artistLabel.trailingAnchor.constraintEqualToAnchor(titleLabel.trailingAnchor),

      // Time (below artist, pinned to bottom → completes vertical chain)
      timeLabel.topAnchor.constraintEqualToAnchor(artistLabel.bottomAnchor, constant = 4.0),
      timeLabel.leadingAnchor.constraintEqualToAnchor(titleLabel.leadingAnchor),
      timeLabel.trailingAnchor.constraintEqualToAnchor(titleLabel.trailingAnchor),
      timeLabel.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -padding),
    ))
  }

  @ObjCAction fun favoriteClicked(sender: platform.darwin.NSObject?) = viewModel.toggleFavorite()
  @ObjCAction fun prevClicked(sender: platform.darwin.NSObject?) = viewModel.previousTrack()
  @ObjCAction fun playPauseClicked(sender: platform.darwin.NSObject?) = viewModel.togglePlayPause()
  @ObjCAction fun nextClicked(sender: platform.darwin.NSObject?) = viewModel.nextTrack()

  private fun updateUI() {
    val running = viewModel.isRunning

    backgroundImageView.setHidden(!running)
    blurView.setHidden(!running)
    if (running) backgroundImageView.image = viewModel.track.albumArt

    notRunningLabel.setHidden(running)
    notRunningLabel.stringValue = "Play something on\n${viewModel.name}"

    listOf(albumArtContainer, controlsGlass, titleLabel, artistLabel, timeLabel).forEach {
      it.setHidden(!running)
    }

    if (running) {
      albumArtImageView.image = viewModel.track.albumArt
      titleLabel.stringValue = viewModel.track.title
      artistLabel.stringValue = viewModel.track.artist
      timeLabel.stringValue = "${viewModel.formatSecondsForDisplay(viewModel.seekerPosition)} / ${viewModel.formatSecondsForDisplay(viewModel.trackDuration)}"

      val heartIcon = if (viewModel.isFavorited) "heart.fill" else "heart"
      favoriteButton.image = NSImage.imageWithSystemSymbolName(heartIcon, accessibilityDescription = null)

      val playIcon = if (viewModel.isPlaying) "pause.fill" else "play.fill"
      playPauseButton.image = NSImage.imageWithSystemSymbolName(playIcon, accessibilityDescription = null)
    }
  }
}
