package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.Constants
import platform.AppKit.NSAppearance
import platform.AppKit.NSAppearanceNameVibrantDark
import platform.AppKit.NSAppearanceNameVibrantLight
import platform.AppKit.NSColor
import platform.AppKit.NSScreen
import platform.AppKit.NSView
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNumber
import platform.QuartzCore.CABasicAnimation
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCACornerCurveContinuous

// Fields are not supported for Companion of subclass of ObjC type.
internal val VIBRANT_APPEARANCES = listOf(NSAppearanceNameVibrantDark, NSAppearanceNameVibrantLight)

public class StatusBarAnimation(
  menubarAppearance: NSAppearance,
  private val menubarHeight: Double,
  isPlaying: Boolean,
) : NSView(
  CGRectMake(
    Constants.StatusBar.statusBarButtonPadding,
    0.0,
    Constants.StatusBar.barAnimationWidth,
    menubarHeight,
  ),
) {
  public var menubarIsDarkAppearance: Boolean =
    menubarAppearance.bestMatchFromAppearancesWithNames(VIBRANT_APPEARANCES) == NSAppearanceNameVibrantDark
    set(value) {
      field = value
      animate()
      needsDisplay = true
    }

  override fun viewDidChangeEffectiveAppearance() {
    menubarIsDarkAppearance =
      effectiveAppearance.bestMatchFromAppearancesWithNames(VIBRANT_APPEARANCES) == NSAppearanceNameVibrantDark
  }

  public var isPlaying: Boolean = isPlaying
    set(value) {
      field = value
      animate()
      needsDisplay = true
    }

  private val backgroundColor
    get() = if (menubarIsDarkAppearance) NSColor.whiteColor.CGColor else NSColor.blackColor.CGColor

  private val bars = mutableListOf<CALayer>()
  private val barHeights = doubleArrayOf(7.0, 6.0, 9.0, 8.0)
  private val barDurations = doubleArrayOf(0.6, 0.3, 0.5, 0.7)

  override fun wantsUpdateLayer(): Boolean = true

  init {
    wantsLayer = true
    layer?.contentsScale = NSScreen.mainScreen?.backingScaleFactor ?: 2.0
    animate()
  }

  override fun viewDidMoveToWindow() {
    super.viewDidMoveToWindow()

    // 윈도우 연결 후 올바른 backingScaleFactor 반영
    window?.backingScaleFactor?.let { scale ->
      layer?.contentsScale = scale
      bars.forEach { it.contentsScale = scale }
    }
  }

  public fun animate() {
    layer?.sublayers?.toList()?.forEach { (it as CALayer).removeFromSuperlayer() }
    bars.clear()

    for (i in barHeights.indices) {
      // Paused state: 처음 2개의 바만 정적으로 표시
      if (!isPlaying && i >= 2) break

      val bar = CALayer().apply {
        NSScreen.mainScreen?.backingScaleFactor?.let { contentsScale = it }
        backgroundColor = this@StatusBarAnimation.backgroundColor
        cornerRadius = if (this@StatusBarAnimation.isPlaying) 1.0 else 2.0
        cornerCurve = kCACornerCurveContinuous
        anchorPoint = CGPointMake(0.0, 0.0)
        frame = CGRectMake(
          x = if (this@StatusBarAnimation.isPlaying) i * 3.5 else i * 8.0,
          y = (menubarHeight / 2) - 5,
          width = if (this@StatusBarAnimation.isPlaying) 2.0 else 6.0,
          height = if (this@StatusBarAnimation.isPlaying) barHeights[i] else 10.0,
        )
      }
      layer?.addSublayer(bar)

      if (!isPlaying) continue

      val animation = CABasicAnimation.animationWithKeyPath("bounds.size.height").apply {
        fromValue = NSNumber(double = barHeights[i])
        toValue = NSNumber(double = 2.0)
        duration = barDurations[i]
        autoreverses = true
        repeatCount = Float.MAX_VALUE
        beginTime = CACurrentMediaTime() - i.toDouble()
      }
      bar.addAnimation(animation, forKey = null)
      bars.add(bar)
    }
  }

  override fun updateLayer() {
    bars.forEach { it.backgroundColor = backgroundColor }
  }
}
