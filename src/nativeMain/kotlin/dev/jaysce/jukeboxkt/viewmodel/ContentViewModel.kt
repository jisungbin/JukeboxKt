package dev.jaysce.jukeboxkt.viewmodel

import DistributedNotificationHelper.JBAddDistributedNotificationObserverWithDeliverImmediately
import dev.jaysce.jukeboxkt.bridge.MusicBridge
import dev.jaysce.jukeboxkt.model.Track
import dev.jaysce.jukeboxkt.util.Constants
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDate
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimer
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

public class ContentViewModel : NSObject() {
  private val musicBridge = MusicBridge()

  public val name: String
    get() = Constants.AppleMusic.name

  public var track: Track = Track()
    private set

  public val isRunning: Boolean
    get() = musicBridge.isRunning

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

  public fun setup() {
    setupObservers()
    if (musicBridge.isRunning) playStateOrTrackDidChange()
  }

  public fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  public fun previousTrack() {
    musicBridge.previousTrack()
  }

  public fun nextTrack() {
    musicBridge.nextTrack()
  }

  public fun togglePlayPause() {
    isPlaying = !isPlaying
    notifyListeners()
    musicBridge.playPause()
  }

  public fun toggleFavorite() {
    isFavorited = !isFavorited
    lastFavoriteToggleTime = NSDate().timeIntervalSince1970
    notifyListeners()
    musicBridge.setFavorited(isFavorited)
  }

  public fun formatSecondsForDisplay(seconds: Double): String {
    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    else "$minutes:${secs.toString().padStart(2, '0')}"
  }

  private fun setupObservers() {
    JBAddDistributedNotificationObserverWithDeliverImmediately(
      observer = this,
      selector = NSSelectorFromString("handleDistributedNotification:"),
      name = Constants.AppleMusic.notification,
      `object` = null,
    )

    NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverWillShowNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) {
      startTimer()
    }

    NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverDidCloseNotification",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) {
      pauseTimer()
    }
  }

  private fun notifyListeners() {
    listeners.forEach { it() }
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

  private fun updateMenuBarText() {
    NSNotificationCenter.defaultCenter.postNotificationName(
      aName = "TrackChanged",
      `object` = null,
      userInfo = mapOf(
        "title" to track.title,
        "artist" to track.artist,
        "isPlaying" to isPlaying,
      ),
    )
  }

  private fun getCurrentSeekerPosition() {
    if (!musicBridge.isRunning) return
    seekerPosition = musicBridge.getPlayerPosition()
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

  @ObjCAction public fun handleDistributedNotification(notification: NSNotification?) {
    playStateOrTrackDidChange(notification)
  }
}
