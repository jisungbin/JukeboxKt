package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.Constants
import dev.jaysce.jukeboxkt.util.PermissionStatus.CLOSED
import dev.jaysce.jukeboxkt.util.PermissionStatus.DENIED
import dev.jaysce.jukeboxkt.util.PermissionStatus.GRANTED
import dev.jaysce.jukeboxkt.util.PermissionStatus.NOT_PROMPTED
import dev.jaysce.jukeboxkt.util.label
import dev.jaysce.jukeboxkt.util.promptUserForConsent
import kotlinx.cinterop.ObjCAction
import platform.AppKit.NSAlert
import platform.AppKit.NSBezelStyleAccessoryBarAction
import platform.AppKit.NSBezelStyleInline
import platform.AppKit.NSBezelStyleRounded
import platform.AppKit.NSButton
import platform.AppKit.NSButtonTypeSwitch
import platform.AppKit.NSColor
import platform.AppKit.NSFont
import platform.AppKit.NSFontWeightSemibold
import platform.AppKit.NSImage
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSView
import platform.AppKit.NSVisualEffectBlendingMode
import platform.AppKit.NSVisualEffectMaterialSidebar
import platform.AppKit.NSVisualEffectView
import platform.AppKit.NSWindow
import platform.AppKit.NSWorkspace
import platform.Foundation.NSMakeRect
import platform.Foundation.NSSelectorFromString
import platform.ServiceManagement.SMAppService
import platform.ServiceManagement.SMAppServiceStatus
import platform.darwin.NSObject

public class PreferencesContentView(private val parentWindow: NSWindow) : NSView(NSMakeRect(0.0, 0.0, 400.0, 164.0)) {
  private val launchAtLoginCheckbox = NSButton()

  init {
    wantsLayer = true
    setupHeader()
    setupPreferencePanes()
  }

  private fun setupHeader() {
    val headerBlur = NSVisualEffectView(NSMakeRect(0.0, 104.0, 400.0, 60.0)).apply {
      material = NSVisualEffectMaterialSidebar
      blendingMode = NSVisualEffectBlendingMode.NSVisualEffectBlendingModeBehindWindow
    }
    addSubview(headerBlur)

    val closeButton = NSButton(NSMakeRect(8.0, 122.0, 24.0, 24.0)).apply {
      bezelStyle = NSBezelStyleAccessoryBarAction
      setBordered(false)
      image = NSImage.imageWithSystemSymbolName("xmark.circle.fill", accessibilityDescription = null)
      target = this@PreferencesContentView
      action = NSSelectorFromString("closeWindow:")
    }
    addSubview(closeButton)

    val iconView = NSImageView(NSMakeRect(140.0, 114.0, 40.0, 40.0)).apply {
      image = NSImage.imageNamed("AppIcon")
      imageScaling = NSImageScaleProportionallyUpOrDown
    }
    addSubview(iconView)

    addSubview(
      label("Jukebox.kt").apply {
        font = NSFont.boldSystemFontOfSize(14.0)
        frame = NSMakeRect(185.0, 130.0, 80.0, 18.0)
      },
    )

    addSubview(
      label("Version ${Constants.AppInfo.appVersion ?: '?'}").apply {
        font = NSFont.systemFontOfSize(11.0)
        textColor = NSColor.secondaryLabelColor
        frame = NSMakeRect(185.0, 114.0, 100.0, 14.0)
      },
    )

    addSubview(
      NSButton(NSMakeRect(300.0, 130.0, 60.0, 20.0)).apply {
        title = "GitHub"
        bezelStyle = NSBezelStyleInline
        target = this@PreferencesContentView
        action = NSSelectorFromString("openGitHub:")
      },
    )

    addSubview(
      NSButton(NSMakeRect(300.0, 110.0, 60.0, 20.0)).apply {
        title = "Website"
        bezelStyle = NSBezelStyleInline
        target = this@PreferencesContentView
        action = NSSelectorFromString("openWebsite:")
      },
    )
  }

  private fun setupPreferencePanes() {
    addSubview(
      label("General").apply {
        font = NSFont.systemFontOfSize(17.0, weight = NSFontWeightSemibold)
        frame = NSMakeRect(16.0, 68.0, 200.0, 22.0)
      },
    )

    launchAtLoginCheckbox.apply {
      setButtonType(NSButtonTypeSwitch)
      title = "Launch at Login"
      frame = NSMakeRect(16.0, 44.0, 200.0, 20.0)
      state = if (SMAppService.mainAppService.status == SMAppServiceStatus.SMAppServiceStatusEnabled) 1 else 0
      target = this@PreferencesContentView
      action = NSSelectorFromString("toggleLaunchAtLogin:")
    }
    addSubview(launchAtLoginCheckbox)

    addSubview(
      NSButton(NSMakeRect(16.0, 16.0, 180.0, 24.0)).apply {
        title = "Check permissions..."
        bezelStyle = NSBezelStyleRounded
        target = this@PreferencesContentView
        action = NSSelectorFromString("checkPermissions:")
      },
    )
  }

  @ObjCAction public fun closeWindow(sender: NSObject?) {
    parentWindow.close()
  }

  @ObjCAction public fun openGitHub(sender: NSObject?) {
    NSWorkspace.sharedWorkspace.openURL(Constants.AppInfo.repo)
  }

  @ObjCAction public fun openWebsite(sender: NSObject?) {
    NSWorkspace.sharedWorkspace.openURL(Constants.AppInfo.website)
  }

  @ObjCAction public fun toggleLaunchAtLogin(sender: NSObject?) {
    val service = SMAppService.mainAppService

    if (launchAtLoginCheckbox.state == 1L)
      service.registerAndReturnError(null)
    else
      service.unregisterAndReturnError(null)
  }

  @ObjCAction public fun checkPermissions(sender: NSObject?) {
    val consent = promptUserForConsent(Constants.AppleMusic.bundleID)
    val alert = NSAlert()
    when (consent) {
      CLOSED -> {
        alert.messageText = "${Constants.AppleMusic.name} is not open"
        alert.informativeText = "Please open ${Constants.AppleMusic.name} to enable permissions"
      }
      GRANTED -> {
        alert.messageText = "Permission granted for ${Constants.AppleMusic.name}"
        alert.informativeText = "Start playing a song!"
      }
      DENIED -> {
        alert.messageText = "Permission denied"
        alert.informativeText =
          "Please go to System Settings > Privacy & Security > Automation, " +
            "and check ${Constants.AppleMusic.name} under Jukebox.kt"
      }
      NOT_PROMPTED -> return
    }
    alert.addButtonWithTitle("Got it!")
    alert.runModal()
  }
}
