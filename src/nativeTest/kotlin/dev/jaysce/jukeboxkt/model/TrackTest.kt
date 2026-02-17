package dev.jaysce.jukeboxkt.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackTest {
  @Test
  fun defaultValues() {
    val track = Track()
    assertEquals("", track.title)
    assertEquals("", track.artist)
    assertEquals("", track.album)
    assertFalse(track.loved)
    assertNull(track.albumArt)
  }

  @Test
  fun customValues() {
    val track = Track(
      title = "Song",
      artist = "Artist",
      album = "Album",
      loved = true,
    )
    assertEquals("Song", track.title)
    assertEquals("Artist", track.artist)
    assertEquals("Album", track.album)
    assertTrue(track.loved)
  }

  @Test
  fun copy() {
    val track = Track(title = "A", artist = "B")
    val copy = track.copy(title = "C")
    assertEquals("C", copy.title)
    assertEquals("B", copy.artist)
  }

  @Test
  fun equality() {
    val a = Track(title = "X", artist = "Y")
    val b = Track(title = "X", artist = "Y")
    assertEquals(a, b)
  }

  @Test
  fun immutabilityCopy() {
    val track = Track()
    val updated = track.copy(title = "Updated", artist = "Updated Artist", loved = true)
    assertEquals("Updated", updated.title)
    assertEquals("Updated Artist", updated.artist)
    assertTrue(updated.loved)
    // 원본은 불변
    assertEquals("", track.title)
  }
}
