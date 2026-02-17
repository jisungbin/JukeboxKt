package dev.jaysce.jukeboxkt.viewmodel

import DistributedNotificationHelper.JBAddDistributedNotificationObserverWithDeliverImmediately
import dev.jaysce.jukeboxkt.bridge.MusicBridge
import dev.jaysce.jukeboxkt.model.Track
import dev.jaysce.jukeboxkt.util.Constants
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimer
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

public class ContentViewModel : NSObject() {
  private val musicBridge = MusicBridge()

  public var track: Track = Track()
    private set
  public var isPlaying: Boolean = false
    private set
  public var isFavorited: Boolean = false
    private set

  public var trackDuration: Double = 0.0
    private set
  public var seekerPosition: Double = 0.0
    private set

  private var seekerTimer: NSTimer? = null
  private var lastFavoriteToggleTime: Double = 0.0
  private val listeners = mutableListOf<() -> Unit>()

  public fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  private fun notifyListeners() {
    listeners.forEach { it() }
  }

  public fun setup() {
    setupObservers()
    if (musicBridge.isRunning) playStateOrTrackDidChange()
  }

  @ObjCAction
  public fun handleDistributedNotification(notification: NSNotification?) {
    playStateOrTrackDidChange(notification)
  }

  private fun setupObservers() {
    JBAddDistributedNotificationObserverWithDeliverImmediately(
      this,
      NSSelectorFromString("handleDistributedNotification:"),
      Constants.AppleMusic.notification,
      null,
    )

    NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverWillShowNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { startTimer() }

    NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverDidCloseNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { pauseTimer() }
  }

  private fun playStateOrTrackDidChange(notification: NSNotification? = null) {
    val playerState = notification?.userInfo?.get("Player State") as? String
    if (!musicBridge.isRunning || playerState == "Stopped") {
      track = Track()
      trackDuration = 0.0
      updateMenuBarText()
      notifyListeners()
      return
    }
    isPlaying = musicBridge.isPlaying()
    getTrackInformation()
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

  public fun togglePlayPause() {
    isPlaying = !isPlaying
    notifyListeners()
    musicBridge.playPause()
  }

  public fun previousTrack(): Unit = musicBridge.previousTrack()
  public fun nextTrack(): Unit = musicBridge.nextTrack()

  public fun toggleFavorite() {
    isFavorited = !isFavorited
    lastFavoriteToggleTime = NSDate().timeIntervalSince1970
    notifyListeners()
    musicBridge.setFavorited(isFavorited)
  }

  private fun getCurrentSeekerPosition() {
    if (!musicBridge.isRunning) return
    seekerPosition = musicBridge.getPlayerPosition()
  }

  private fun startTimer() {
    pauseTimer()
    seekerTimer = NSTimer.scheduledTimerWithTimeInterval(0.1, repeats = true) {
      getCurrentSeekerPosition()
      notifyListeners()
    }
  }

  private fun pauseTimer() {
    seekerTimer?.invalidate()
    seekerTimer = null
  }

  public val isRunning: Boolean get() = musicBridge.isRunning
  public val name: String get() = Constants.AppleMusic.name

  public fun formatSecondsForDisplay(seconds: Double): String {
    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    else "$minutes:${secs.toString().padStart(2, '0')}"
  }
}
