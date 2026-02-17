package dev.jaysce.jukeboxkt.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MusicBridgeTest {
  @Test
  fun bridgeCreation() {
    val bridge = MusicBridge()
    assertNotNull(bridge)
  }

  @Test
  fun isRunningWhenMusicNotOpen() {
    // This test verifies the bridge doesn't crash when Music is not running.
    // The actual return value depends on whether Apple Music is open.
    val bridge = MusicBridge()
    // Just verify it doesn't throw
    bridge.isRunning
  }

  @Test
  fun getCurrentTrackWhenNotRunning() {
    val bridge = MusicBridge()
    if (!bridge.isRunning) {
      val track = bridge.getCurrentTrack()
      assertNull(track)
    }
  }

  @Test
  fun getPlayerPositionWhenNotRunning() {
    val bridge = MusicBridge()
    if (!bridge.isRunning) {
      assertEquals(0.0, bridge.getPlayerPosition())
    }
  }

  @Test
  fun isPlayingWhenNotRunning() {
    val bridge = MusicBridge()
    if (!bridge.isRunning) {
      assertFalse(bridge.isPlaying())
    }
  }
}
