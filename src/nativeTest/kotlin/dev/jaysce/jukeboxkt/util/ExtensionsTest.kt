package dev.jaysce.jukeboxkt.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.AppKit.NSFont

class ExtensionsTest {
  @Test
  fun stringWidthNonEmpty() {
    val font = NSFont.systemFontOfSize(13.0)
    val width = "Hello World".stringWidth(font)
    assertTrue(width > 0, "String width should be positive")
  }

  @Test
  fun stringWidthEmpty() {
    val font = NSFont.systemFontOfSize(13.0)
    val width = "".stringWidth(font)
    assertEquals(0.0, width)
  }

  @Test
  fun stringHeightNonEmpty() {
    val font = NSFont.systemFontOfSize(13.0)
    val height = "Hello".stringHeight(font)
    assertTrue(height > 0, "String height should be positive")
  }
}
