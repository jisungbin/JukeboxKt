package dev.jaysce.jukeboxkt.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConstantsTest {
  @Test
  fun appleMusicConstants() {
    assertEquals("Apple Music", Constants.AppleMusic.name)
    assertEquals("com.apple.Music", Constants.AppleMusic.bundleID)
    assertEquals("com.apple.Music.playerInfo", Constants.AppleMusic.notification)
  }

  @Test
  fun statusBarConstants() {
    assertNotNull(Constants.StatusBar.marqueeFont)
    assertEquals(14.0, Constants.StatusBar.barAnimationWidth)
    assertEquals(200.0, Constants.StatusBar.statusBarButtonLimit)
    assertEquals(8.0, Constants.StatusBar.statusBarButtonPadding)
  }

  @Test
  fun appInfoUrls() {
    assertNotNull(Constants.AppInfo.repo)
    assertNotNull(Constants.AppInfo.website)
    assertTrue(Constants.AppInfo.repo.absoluteString!!.contains("github.com"))
    assertTrue(Constants.AppInfo.website.absoluteString!!.contains("jaysce.dev"))
  }
}
