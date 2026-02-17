package dev.jaysce.jukeboxkt.app

import dev.jaysce.jukeboxkt.util.Constants
import dev.jaysce.jukeboxkt.util.stringWidth
import dev.jaysce.jukeboxkt.view.AboutMenuItemView
import dev.jaysce.jukeboxkt.view.ContentPopoverView
import dev.jaysce.jukeboxkt.view.MenuMarqueeText
import dev.jaysce.jukeboxkt.view.OnboardingContentView
import dev.jaysce.jukeboxkt.view.PreferencesContentView
import dev.jaysce.jukeboxkt.view.StatusBarAnimation
import dev.jaysce.jukeboxkt.viewmodel.ContentViewModel
import dev.jaysce.jukeboxkt.window.OnboardingWindow
import dev.jaysce.jukeboxkt.window.PreferencesWindow
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.AppKit.NSApp
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSEventTypeRightMouseUp
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuDelegateProtocol
import platform.AppKit.NSMenuItem
import platform.AppKit.NSPopover
import platform.AppKit.NSPopoverBehaviorTransient
import platform.AppKit.NSStatusBar
import platform.AppKit.NSStatusBarButton
import platform.AppKit.NSStatusItem
import platform.AppKit.NSViewController
import platform.AppKit.currentEvent
import platform.Foundation.NSDate
import platform.Foundation.NSMakeRect
import platform.Foundation.NSMinYEdge
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

class AppDelegate : NSObject(), NSApplicationDelegateProtocol, NSMenuDelegateProtocol {

  private val defaults = NSUserDefaults.standardUserDefaults
  private val viewModel = ContentViewModel()

  private var statusBarItem: NSStatusItem? = null
  private var statusBarMenu: NSMenu? = null
  private var popover: NSPopover? = null
  private var preferencesWindow: PreferencesWindow? = null
  private var onboardingWindow: OnboardingWindow? = null

  private var trackChangedObserver: Any? = null
  private var popoverCloseObserver: Any? = null
  private var lastPopoverCloseTime: Double = 0.0

  override fun applicationDidFinishLaunching(notification: NSNotification) {
    trackChangedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
      name = "TrackChanged",
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { notif ->
      if (notif != null) updateStatusBarItemTitle(notif)
    }

    if (!defaults.boolForKey("viewedOnboarding")) {
      showOnboarding()
      NSApplication.sharedApplication.activate()
      return
    }

    setupContentView()
    setupStatusBar()
    viewModel.setup()
  }

  private fun setupContentView() {
    val contentView = ContentPopoverView(viewModel)

    popover = NSPopover().apply {
      behavior = NSPopoverBehaviorTransient
      animates = true
      contentViewController = NSViewController().apply {
        view = contentView
      }
    }

    popoverCloseObserver = NSNotificationCenter.defaultCenter.addObserverForName(
      name = "NSPopoverDidCloseNotification",
      `object` = popover,
      queue = NSOperationQueue.mainQueue,
    ) {
      lastPopoverCloseTime = NSDate().timeIntervalSince1970
    }
  }

  private fun setupStatusBar() {
    statusBarMenu = NSMenu().apply {
      delegate = this@AppDelegate

      val aboutMenuItem = NSMenuItem()
      aboutMenuItem.view = AboutMenuItemView()
      addItem(aboutMenuItem)
      addItem(NSMenuItem.separatorItem())
      addItem(
        NSMenuItem(
          title = "Preferences...",
          action = NSSelectorFromString("showPreferences:"),
          keyEquivalent = "",
        ).apply { target = this@AppDelegate },
      )
      addItem(
        NSMenuItem(
          title = "Quit Jukebox.kt",
          action = NSSelectorFromString("terminate:"),
          keyEquivalent = "",
        ).apply { target = NSApplication.sharedApplication },
      )
    }

    statusBarItem = NSStatusBar.systemStatusBar.statusItemWithLength(-1.0)

    statusBarItem?.button?.let { button ->
      val barAnimation = StatusBarAnimation(
        menubarAppearance = button.effectiveAppearance,
        menubarHeight = button.bounds.useContents { size.height },
        isPlaying = false,
      )
      button.addSubview(barAnimation)

      val marqueeText = MenuMarqueeText(
        text = "",
        menubarBounds = button.bounds,
        menubarAppearance = button.effectiveAppearance,
      )
      button.addSubview(marqueeText)

      val animWidth = barAnimation.bounds.useContents { size.width }
      val buttonHeight = button.bounds.useContents { size.height }
      button.frame = NSMakeRect(0.0, 0.0, animWidth + 16, buttonHeight)
      marqueeText.menubarBounds = button.bounds

      button.target = this
      button.action = NSSelectorFromString("didClickStatusBarItem:")
      // NSEventMaskLeftMouseUp (4) | NSEventMaskRightMouseUp (16)
      button.sendActionOn(20uL)
    }
  }

  @ObjCAction
  fun didClickStatusBarItem(sender: NSObject?) {
    val event = NSApp?.currentEvent ?: return
    if (event.type == NSEventTypeRightMouseUp) {
      statusBarItem?.menu = statusBarMenu
      statusBarItem?.button?.performClick(null)
    } else {
      togglePopover(statusBarItem?.button)
    }
  }

  override fun menuDidClose(menu: NSMenu) {
    statusBarItem?.menu = null
  }

  private fun togglePopover(sender: NSStatusBarButton?) {
    val button = sender ?: return
    val pop = popover ?: return

    if (pop.isShown()) {
      pop.performClose(button)
    } else {
      // Prevent re-opening if just closed by transient behavior
      if (NSDate().timeIntervalSince1970 - lastPopoverCloseTime < 0.3) return
      pop.showRelativeToRect(button.bounds, ofView = button, preferredEdge = NSMinYEdge)
      pop.contentViewController?.view?.window?.makeKeyWindow()
      NSApplication.sharedApplication.activate()
    }
  }

  private fun updateStatusBarItemTitle(notification: NSNotification) {
    val trackTitle = notification.userInfo?.get("title") as? String ?: return
    val trackArtist = notification.userInfo?.get("artist") as? String ?: return
    val isPlaying = notification.userInfo?.get("isPlaying") as? Boolean ?: return
    val titleAndArtist = if (trackTitle.isEmpty() && trackArtist.isEmpty()) "" else "$trackTitle \u2022 $trackArtist"

    val button = statusBarItem?.button ?: return
    val barAnimation = button.subviews.getOrNull(0) as? StatusBarAnimation ?: return
    val marqueeText = button.subviews.getOrNull(1) as? MenuMarqueeText ?: return

    val font = Constants.StatusBar.marqueeFont
    val stringWidth = titleAndArtist.stringWidth(font)
    marqueeText.text = titleAndArtist

    val limit = Constants.StatusBar.statusBarButtonLimit
    val animWidth = Constants.StatusBar.barAnimationWidth
    val padding = Constants.StatusBar.statusBarButtonPadding
    val buttonHeight = button.bounds.useContents { size.height }

    if (titleAndArtist.isEmpty()) {
      barAnimation.isPlaying = false
      val barWidth = barAnimation.bounds.useContents { size.width }
      button.frame = NSMakeRect(0.0, 0.0, barWidth + 16, buttonHeight)
      return
    }

    val totalWidth = if (stringWidth < limit) {
      stringWidth + animWidth + 3 * padding
    } else {
      limit + animWidth + 3 * padding
    }
    button.frame = NSMakeRect(0.0, 0.0, totalWidth, buttonHeight)
    barAnimation.isPlaying = isPlaying
    marqueeText.menubarBounds = button.bounds
  }

  @ObjCAction
  fun showPreferences(sender: NSObject?) {
    if (preferencesWindow == null) {
      preferencesWindow = PreferencesWindow()
      preferencesWindow!!.contentView = PreferencesContentView(preferencesWindow!!)
    }
    preferencesWindow!!.center()
    preferencesWindow!!.makeKeyAndOrderFront(null)
    NSApplication.sharedApplication.activate()
  }

  private fun showOnboarding() {
    if (onboardingWindow == null) {
      onboardingWindow = OnboardingWindow()
      onboardingWindow!!.contentView = OnboardingContentView {
        finishOnboarding()
      }
    }
    onboardingWindow!!.center()
    onboardingWindow!!.makeKeyAndOrderFront(null)
    NSApplication.sharedApplication.activate()
  }

  private fun finishOnboarding() {
    setupContentView()
    setupStatusBar()
    viewModel.setup()
    onboardingWindow?.close()
    onboardingWindow = null
  }
}
