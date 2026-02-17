package dev.jaysce.jukeboxkt.util

import platform.AppKit.NSFont
import platform.AppKit.NSFontWeightMedium
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

public object Constants {
  public object AppInfo {
    public val appVersion: String?
      get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    public val repo: NSURL = NSURL(string = "https://github.com/Jaysce/Jukebox")
    public val website: NSURL = NSURL(string = "https://jaysce.dev/projects/jukebox")
  }

  public object StatusBar {
    public val marqueeFont: NSFont = NSFont.systemFontOfSize(13.0, weight = NSFontWeightMedium)
    public const val barAnimationWidth: Double = 14.0
    public const val statusBarButtonLimit: Double = 200.0
    public const val statusBarButtonPadding: Double = 8.0
  }

  public object AppleMusic {
    public const val name: String = "Apple Music"
    public const val bundleID: String = "com.apple.Music"
    public val notification: String = "$bundleID.playerInfo"
  }
}
