package dev.jaysce.jukeboxkt.model

import platform.AppKit.NSImage

public data class Track(
  public val title: String = "",
  public val artist: String = "",
  public val album: String = "",
  public val albumArt: NSImage? = null,
)
