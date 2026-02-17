package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.Constants
import dev.jaysce.jukeboxkt.util.PermissionStatus
import dev.jaysce.jukeboxkt.util.label
import dev.jaysce.jukeboxkt.util.promptUserForConsent
import kotlinx.cinterop.ObjCAction
import platform.AppKit.NSAlert
import platform.AppKit.NSApp
import platform.AppKit.NSBezelStyleRounded
import platform.AppKit.NSButton
import platform.AppKit.NSColor
import platform.AppKit.NSFont
import platform.AppKit.NSImage
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSView
import platform.AppKit.NSVisualEffectBlendingMode
import platform.AppKit.NSVisualEffectMaterialPopover
import platform.AppKit.NSVisualEffectView
import platform.Foundation.NSMakeRect
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSUserDefaults
import platform.MetalKit.MTKView

public class OnboardingContentView(
  private val onFinish: () -> Unit,
) : NSView(NSMakeRect(0.0, 0.0, 500.0, 200.0)) {

  private val defaults = NSUserDefaults.standardUserDefaults
  private val metalView = MTKView(NSMakeRect(0.0, 0.0, 250.0, 200.0))
  private var metalRenderer: MetalRenderer? = null
  private val continueButton = NSButton()

  init {
    wantsLayer = true
    setupLeftPanel()
    setupRightPanel()
  }

  private fun setupLeftPanel() {
    metalRenderer = MetalRenderer(functionName = "warp", mtkView = metalView)
    addSubview(metalView)

    addSubview(NSVisualEffectView(NSMakeRect(0.0, 0.0, 250.0, 200.0)).apply {
      material = NSVisualEffectMaterialPopover
      blendingMode = NSVisualEffectBlendingMode.NSVisualEffectBlendingModeWithinWindow
    })

    addSubview(NSImageView(NSMakeRect(95.0, 70.0, 60.0, 60.0)).apply {
      image = NSImage.imageNamed("AppIcon")
      imageScaling = NSImageScaleProportionallyUpOrDown
    })
  }

  private fun setupRightPanel() {
    addSubview(label("Jukebox.kt").apply {
      font = NSFont.boldSystemFontOfSize(16.0)
      frame = NSMakeRect(270.0, 150.0, 210.0, 22.0)
      alignment = NSTextAlignmentCenter
    })

    addSubview(
      label(
        "Jukebox.kt requires permission to control Apple Music and display music data.\n\nPlease open Apple Music and click Continue.",
      ).apply {
        font = NSFont.systemFontOfSize(11.0)
        textColor = NSColor.secondaryLabelColor
        frame = NSMakeRect(270.0, 70.0, 210.0, 75.0)
        alignment = NSTextAlignmentCenter
        maximumNumberOfLines = 5
      })

    addSubview(NSView(NSMakeRect(250.0, 40.0, 250.0, 1.0)).apply {
      wantsLayer = true
      layer?.backgroundColor = NSColor.separatorColor.CGColor
    })

    addSubview(NSButton(NSMakeRect(290.0, 8.0, 80.0, 28.0)).apply {
      title = "Quit"
      bezelStyle = NSBezelStyleRounded
      target = this@OnboardingContentView
      action = NSSelectorFromString("quitApp:")
    })

    continueButton.apply {
      title = "Continue"
      bezelStyle = NSBezelStyleRounded
      frame = NSMakeRect(380.0, 8.0, 100.0, 28.0)
      keyEquivalent = "\r"
      target = this@OnboardingContentView
      action = NSSelectorFromString("continueClicked:")
    }
    addSubview(continueButton)
  }

  @ObjCAction public fun quitApp(sender: platform.darwin.NSObject?) {
    NSApp?.terminate(this)
  }

  @ObjCAction public fun continueClicked(sender: platform.darwin.NSObject?) {
    when (promptUserForConsent(Constants.AppleMusic.bundleID)) {
      PermissionStatus.GRANTED -> {
        defaults.setBool(true, forKey = "viewedOnboarding")
        metalRenderer?.setPaused(true)
        onFinish()
      }
      PermissionStatus.CLOSED -> showAlert(
        "${Constants.AppleMusic.name} is not open",
        "Please open ${Constants.AppleMusic.name} to enable permissions",
      )
      PermissionStatus.DENIED -> showAlert(
        "Permission denied",
        "Please go to System Settings > Privacy & Security > Automation, and check Apple Music under Jukebox.kt",
      )
      PermissionStatus.NOT_PROMPTED -> {}
    }
  }

  private fun showAlert(message: String, info: String) {
    NSAlert().apply {
      messageText = message
      informativeText = info
      addButtonWithTitle("Got it!")
      runModal()
    }
  }
}
