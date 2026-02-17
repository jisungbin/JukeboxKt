package dev.jaysce.jukeboxkt.viewmodel

import DistributedNotificationHelper.JBAddDistributedNotificationObserverWithDeliverImmediately
import dev.jaysce.jukeboxkt.bridge.MusicBridge
import dev.jaysce.jukeboxkt.model.Track
import dev.jaysce.jukeboxkt.util.Constants
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDistributedNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSDate
import platform.Foundation.NSTimer
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

class ContentViewModel : NSObject() {
  val musicBridge = MusicBridge()

  var track: Track = Track()
    private set
  var isPlaying: Boolean = false
    private set
  var isFavorited: Boolean = false
    private set

  var trackDuration: Double = 0.0
    private set
  var seekerPosition: Double = 0.0
    private set

  var popoverIsShown: Boolean = true
    private set

  private var seekerTimer: NSTimer? = null
  private var lastFavoriteToggleTime: Double = 0.0
  private val listeners = mutableListOf<() -> Unit>()

  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  private fun notifyListeners() {
    listeners.forEach { it() }
  }

  private var popoverWillShowObserver: Any? = null
  private var popoverDidCloseObserver: Any? = null

  fun setup() {
    setupObservers()
    if (musicBridge.isRunning) playStateOrTrackDidChange()
  }

  fun tearDown() {
    NSDistributedNotificationCenter.defaultCenter.removeObserver(this)
    popoverWillShowObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
    popoverDidCloseObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
    pauseTimer()
  }

  @ObjCAction
  fun handleDistributedNotification(notification: NSNotification?) {
    playStateOrTrackDidChange(notification)
  }

  private fun setupObservers() {
    JBAddDistributedNotificationObserverWithDeliverImmediately(
      this,
      NSSelectorFromString("handleDistributedNotification:"),
      Constants.AppleMusic.notification,
      null,
    )

    popoverWillShowObserver = NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverWillShowNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { startTimer(); popoverIsShown = true }

    popoverDidCloseObserver = NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverDidCloseNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { pauseTimer(); popoverIsShown = false }
  }

  private fun playStateOrTrackDidChange(notification: NSNotification? = null) {
    notification?.userInfo?.let { ui ->
      println("[Jukebox] notification userInfo: ${ui.keys}")
      ui.forEach { (k, v) -> println("[Jukebox]   $k -> $v") }
    }
    val playerState = notification?.userInfo?.get("Player State") as? String
    if (!musicBridge.isRunning || playerState == "Stopped") {
      track = Track()
      trackDuration = 0.0
      updateMenuBarText()
      notifyListeners()
      return
    }
    getPlayState()
    getTrackInformation()
  }

  private fun getPlayState() {
    isPlaying = musicBridge.isPlaying()
  }

  private fun getTrackInformation() {
    musicBridge.getCurrentTrack()?.let { track = it }
    // 수동 토글 후 2초간은 notification의 재읽기를 무시 (Apple Music 반영 지연 대응)
    if (NSDate().timeIntervalSince1970 - lastFavoriteToggleTime > 2.0) {
      isFavorited = musicBridge.isFavorited()
    }
    trackDuration = musicBridge.getTrackDuration()

    musicBridge.getAlbumArtworkWithRetry(
      title = track.title,
      artist = track.artist,
      album = track.album,
    ) { image ->
      if (image != null) {
        track = track.copy(albumArt = image)
        notifyListeners()
      }
    }

    updateMenuBarText()
    notifyListeners()
  }

  private fun updateMenuBarText() {
    NSNotificationCenter.defaultCenter.postNotificationName(
      aName = "TrackChanged",
      `object` = null,
      userInfo = mapOf<Any?, Any?>(
        "title" to track.title,
        "artist" to track.artist,
        "isPlaying" to isPlaying,
      ),
    )
  }

  fun togglePlayPause() {
    isPlaying = !isPlaying
    notifyListeners()
    musicBridge.playPause()
  }
  fun previousTrack() = musicBridge.previousTrack()
  fun nextTrack() = musicBridge.nextTrack()

  fun toggleFavorite() {
    isFavorited = !isFavorited
    lastFavoriteToggleTime = NSDate().timeIntervalSince1970
    notifyListeners()
    musicBridge.setFavorited(isFavorited)
  }

  fun getCurrentSeekerPosition() {
    if (!musicBridge.isRunning) return
    seekerPosition = musicBridge.getPlayerPosition()
  }

  fun startTimer() {
    pauseTimer()
    seekerTimer = NSTimer.scheduledTimerWithTimeInterval(0.1, repeats = true) {
      getCurrentSeekerPosition()
      notifyListeners()
    }
  }

  fun pauseTimer() {
    seekerTimer?.invalidate()
    seekerTimer = null
  }

  val isRunning: Boolean get() = musicBridge.isRunning
  val name: String get() = Constants.AppleMusic.name

  fun formatSecondsForDisplay(seconds: Double): String {
    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) "$hours:$minutes:${secs.toString().padStart(2, '0')}"
    else "$minutes:${secs.toString().padStart(2, '0')}"
  }
}
