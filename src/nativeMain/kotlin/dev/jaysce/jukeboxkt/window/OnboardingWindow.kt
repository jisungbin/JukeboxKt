package dev.jaysce.jukeboxkt.window

import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSFloatingWindowLevel
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.AppKit.NSWindowStyleMaskTitled
import platform.Foundation.NSMakeRect

class OnboardingWindow : NSWindow(
  contentRect = NSMakeRect(0.0, 0.0, 500.0, 200.0),
  styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskFullSizeContentView,
  backing = NSBackingStoreBuffered,
  `defer` = true,
) {
  init {
    titlebarAppearsTransparent = true
    setMovableByWindowBackground(true)
    setReleasedWhenClosed(false)
    level = NSFloatingWindowLevel
  }
}
