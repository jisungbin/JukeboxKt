package dev.jaysce.jukeboxkt.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

  @Test
  fun toBase64() {
    val encoded = "Hello".toBase64()
    assertEquals("SGVsbG8=", encoded)
  }

  @Test
  fun fromBase64() {
    val decoded = "SGVsbG8=".fromBase64()
    assertNotNull(decoded)
    assertEquals("Hello", decoded)
  }

  @Test
  fun fromBase64Invalid() {
    val decoded = "!@#\$%".fromBase64()
    assertNull(decoded)
  }

  @Test
  fun base64Roundtrip() {
    val original = "Jukebox 테스트"
    val encoded = original.toBase64()
    val decoded = encoded.fromBase64()
    assertEquals(original, decoded)
  }
}
