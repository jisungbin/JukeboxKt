package dev.jaysce.jukeboxkt.util

import platform.AppKit.NSFont
import platform.AppKit.NSFontWeightMedium
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

object Constants {
  object AppInfo {
    val appVersion: String?
      get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val repo: NSURL = NSURL(string = "https://github.com/Jaysce/Jukebox")
    val website: NSURL = NSURL(string = "https://jaysce.dev/projects/jukebox")
  }

  object StatusBar {
    val marqueeFont: NSFont = NSFont.systemFontOfSize(13.0, weight = NSFontWeightMedium)
    const val barAnimationWidth: Double = 14.0
    const val statusBarButtonLimit: Double = 200.0
    const val statusBarButtonPadding: Double = 8.0
  }

  object AppleMusic {
    const val name = "Apple Music"
    const val bundleID = "com.apple.Music"
    val notification = "$bundleID.playerInfo"
  }
}
