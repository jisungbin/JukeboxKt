package dev.jaysce.jukeboxkt.util

import kotlinx.cinterop.useContents
import platform.AppKit.NSFont
import platform.AppKit.NSFontAttributeName
import platform.AppKit.NSTextField
import platform.AppKit.size
import platform.CoreGraphics.CGSize
import platform.Foundation.NSAttributedString
import platform.Foundation.create

public fun String.stringWidth(font: NSFont): Double {
  val attrStr = NSAttributedString.create(this, mapOf<Any?, Any?>(NSFontAttributeName to font))
  return attrStr.size().useContents(CGSize::width)
}

public fun String.stringHeight(font: NSFont): Double {
  val attrStr = NSAttributedString.create(this, mapOf<Any?, Any?>(NSFontAttributeName to font))
  return attrStr.size().useContents(CGSize::height)
}

/** 읽기 전용 라벨용 NSTextField 생성 헬퍼 */
public fun label(text: String = ""): NSTextField =
  NSTextField().apply {
    stringValue = text
    setEditable(false)
    setBezeled(false)
    setDrawsBackground(false)
  }
