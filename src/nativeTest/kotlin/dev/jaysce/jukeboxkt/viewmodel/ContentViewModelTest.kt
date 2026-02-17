package dev.jaysce.jukeboxkt.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ContentViewModelTest {
  @Test
  fun initialState() {
    val vm = ContentViewModel()
    assertEquals("", vm.track.title)
    assertEquals("", vm.track.artist)
    assertFalse(vm.isPlaying)
    assertFalse(vm.isFavorited)
    assertEquals(0.0, vm.trackDuration)
    assertEquals(0.0, vm.seekerPosition)
  }

  @Test
  fun name() {
    val vm = ContentViewModel()
    assertEquals("Apple Music", vm.name)
  }

  @Test
  fun formatSeconds_minutesOnly() {
    val vm = ContentViewModel()
    assertEquals("3:45", vm.formatSecondsForDisplay(225.0))
  }

  @Test
  fun formatSeconds_zero() {
    val vm = ContentViewModel()
    assertEquals("0:00", vm.formatSecondsForDisplay(0.0))
  }

  @Test
  fun formatSeconds_withHours() {
    val vm = ContentViewModel()
    // 1 hour, 2 minutes, 30 seconds
    assertEquals("1:02:30", vm.formatSecondsForDisplay(3750.0))
  }

  @Test
  fun listener() {
    val vm = ContentViewModel()
    var called = false
    vm.addListener { called = true }
    assertFalse(called)
  }

  @Test
  fun defaultTrackIsEmpty() {
    val vm = ContentViewModel()
    assertEquals("", vm.track.title)
    assertEquals("", vm.track.artist)
    assertEquals("", vm.track.album)
  }
}
