package dev.jaysce.jukeboxkt.window

import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSFloatingWindowLevel
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.AppKit.NSWindowStyleMaskTitled
import platform.Foundation.NSMakeRect

class PreferencesWindow : NSWindow(
  contentRect = NSMakeRect(0.0, 0.0, 400.0, 164.0),
  styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskFullSizeContentView,
  backing = NSBackingStoreBuffered,
  `defer` = true,
) {
  init {
    level = NSFloatingWindowLevel
    titlebarAppearsTransparent = true
    setMovableByWindowBackground(true)
    setReleasedWhenClosed(false)
  }
}
