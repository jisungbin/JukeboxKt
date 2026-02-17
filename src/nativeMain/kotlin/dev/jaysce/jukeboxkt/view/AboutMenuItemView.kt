package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.Constants
import dev.jaysce.jukeboxkt.util.label
import platform.AppKit.NSApplication
import platform.AppKit.NSColor
import platform.AppKit.NSFont
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSView
import platform.Foundation.NSMakeRect

class AboutMenuItemView : NSView(NSMakeRect(0.0, 0.0, 220.0, 70.0)) {
  init {
    val iconView = NSImageView(NSMakeRect(10.0, 5.0, 60.0, 60.0))
    iconView.image = NSApplication.sharedApplication.applicationIconImage
    iconView.imageScaling = NSImageScaleProportionallyUpOrDown
    addSubview(iconView)

    val nameLabel = label("Jukebox.kt").apply {
      font = NSFont.boldSystemFontOfSize(20.0)
      frame = NSMakeRect(78.0, 35.0, 130.0, 25.0)
    }
    addSubview(nameLabel)

    val versionLabel = label("Version ${Constants.AppInfo.appVersion ?: "?"}").apply {
      font = NSFont.systemFontOfSize(12.0)
      textColor = NSColor.secondaryLabelColor
      frame = NSMakeRect(78.0, 15.0, 130.0, 18.0)
    }
    addSubview(versionLabel)
  }
}
