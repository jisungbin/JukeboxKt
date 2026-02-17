package dev.jaysce.jukeboxkt

import dev.jaysce.jukeboxkt.app.AppDelegate
import platform.AppKit.NSApplication

public fun main() {
  val app = NSApplication.sharedApplication
  val delegate = AppDelegate()
  app.delegate = delegate
  app.run()
}
