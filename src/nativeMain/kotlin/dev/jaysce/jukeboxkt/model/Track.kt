package dev.jaysce.jukeboxkt.model

import platform.AppKit.NSImage

data class Track(
  val title: String = "",
  val artist: String = "",
  val album: String = "",
  val albumArt: NSImage? = null,
)
