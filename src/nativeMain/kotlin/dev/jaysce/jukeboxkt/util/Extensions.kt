package dev.jaysce.jukeboxkt.util

import kotlinx.cinterop.get
import kotlinx.cinterop.useContents
import platform.AppKit.NSBitmapImageRep
import platform.AppKit.NSFont
import platform.AppKit.NSFontAttributeName
import platform.AppKit.NSImage
import platform.AppKit.NSTextField
import platform.AppKit.size
import platform.Foundation.NSAttributedString
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

fun String.stringWidth(font: NSFont): Double {
  val attrStr = NSAttributedString.create(this, mapOf<Any?, Any?>(NSFontAttributeName to font))
  return attrStr.size().useContents { width }
}

fun String.stringHeight(font: NSFont): Double {
  val attrStr = NSAttributedString.create(this, mapOf<Any?, Any?>(NSFontAttributeName to font))
  return attrStr.size().useContents { height }
}

fun String.fromBase64(): String? {
  val data = NSData.create(base64EncodedString = this, options = 0u) ?: return null
  return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
}

fun String.toBase64(): String {
  val data = NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding) ?: return ""
  return data.base64EncodedStringWithOptions(0u)
}

fun NSImage.isEmpty(): Boolean {
  val tiffData = TIFFRepresentation ?: return true
  val rep = NSBitmapImageRep(data = tiffData)
  val width = rep.pixelsWide.toInt()
  val height = rep.pixelsHigh.toInt()
  val bitmapData = rep.bitmapData ?: return true

  for (y in 0 until height) {
    for (x in 0 until width) {
      val offset = ((width * y) + x) * 4
      val a = bitmapData[offset + 3]
      if (a.toInt() != 0) {
        val r = bitmapData[offset]
        val g = bitmapData[offset + 1]
        val b = bitmapData[offset + 2]
        if (r.toInt() != 0 || g.toInt() != 0 || b.toInt() != 0) return false
      }
    }
  }
  return true
}

/** 읽기 전용 라벨용 NSTextField 생성 헬퍼 */
fun label(text: String = ""): NSTextField = NSTextField().apply {
  stringValue = text
  setEditable(false)
  setBezeled(false)
  setDrawsBackground(false)
}
