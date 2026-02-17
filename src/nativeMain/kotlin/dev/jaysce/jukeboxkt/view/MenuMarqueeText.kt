package dev.jaysce.jukeboxkt.view

import dev.jaysce.jukeboxkt.util.Constants
import dev.jaysce.jukeboxkt.util.stringHeight
import dev.jaysce.jukeboxkt.util.stringWidth
import kotlinx.cinterop.CValue
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import platform.AppKit.NSAppearance
import platform.AppKit.NSAppearanceNameVibrantDark
import platform.AppKit.NSColor
import platform.AppKit.NSScreen
import platform.AppKit.NSView
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMakeRect
import platform.QuartzCore.CAAnimationGroup
import platform.QuartzCore.CABasicAnimation
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATextLayer

public class MenuMarqueeText(
  text: String,
  menubarBounds: CValue<CGRect>,
  menubarAppearance: NSAppearance,
) : NSView(menubarBounds) {
  public var text: String = text
    set(value) {
      field = value
      needsDisplay = true
    }

  public var menubarIsDarkAppearance: Boolean =
    menubarAppearance.bestMatchFromAppearancesWithNames(VIBRANT_APPEARANCES) == NSAppearanceNameVibrantDark
    set(value) {
      field = value
      needsDisplay = true
    }

  override fun viewDidChangeEffectiveAppearance() {
    menubarIsDarkAppearance =
      effectiveAppearance.bestMatchFromAppearancesWithNames(VIBRANT_APPEARANCES) == NSAppearanceNameVibrantDark
  }

  public var menubarBounds: CValue<CGRect> = menubarBounds
    set(value) {
      field = value
      needsDisplay = true
    }

  private val foregroundColor
    get() = if (menubarIsDarkAppearance) NSColor.whiteColor.CGColor else NSColor.blackColor.CGColor

  private val padding = 16.0
  private var maskLayer = CALayer()
  private var textLayer1 = CATextLayer()
  private var textLayer2 = CATextLayer()

  override fun wantsUpdateLayer(): Boolean = true

  init {
    wantsLayer = true
    maskLayer = setupMask()
    layer?.addSublayer(maskLayer)
    textLayer1 = setupTextLayer(isFirstLayer = true)
    textLayer2 = setupTextLayer(isFirstLayer = false)
    maskLayer.addSublayer(textLayer1)
    maskLayer.addSublayer(textLayer2)
  }

  private fun setupMask(): CALayer = CALayer().apply {
    menubarBounds.useContents { frame = CGRectMake(0.0, 0.0, size.width, size.height) }
    masksToBounds = true
  }

  private fun setupTextLayer(isFirstLayer: Boolean): CATextLayer {
    val font = Constants.StatusBar.marqueeFont
    val stringWidth = text.stringWidth(font) + padding
    val stringHeight = text.stringHeight(font)
    val boundsHeight = menubarBounds.useContents { size.height }

    val textLayer = CATextLayer().apply {
      string = text
      setFont(interpretCPointer(font.objcPtr()))
      fontSize = font.pointSize
      foregroundColor = this@MenuMarqueeText.foregroundColor
      frame = CGRectMake(
        x = if (isFirstLayer) 0.0 else stringWidth + padding,
        y = (boundsHeight / 2) - (stringHeight / 2),
        width = stringWidth,
        height = stringHeight,
      )
      NSScreen.mainScreen?.backingScaleFactor?.let { contentsScale = it }
    }

    if (stringWidth - padding < 200)
      return textLayer

    val duration = stringWidth / 30
    val delay = 3.0
    val animation = CABasicAnimation.animationWithKeyPath("position.x").apply {
      setFromValue(if (isFirstLayer) stringWidth / 2 else stringWidth + stringWidth / 2)
      setToValue(if (isFirstLayer) -stringWidth + stringWidth / 2 else stringWidth / 2)
      this.duration = duration
      beginTime = delay
    }
    val group = CAAnimationGroup().apply {
      setAnimations(listOf(animation))
      this.duration = duration + delay
      repeatCount = Float.MAX_VALUE
    }

    textLayer.addAnimation(group, forKey = null)
    return textLayer
  }

  override fun updateLayer() {
    frame = menubarBounds

    // toList()로 snapshot 복사 후 제거 (live 참조 순회 중 수정 방지)
    maskLayer.sublayers?.toList()?.forEach { (it as CALayer).removeFromSuperlayer() }

    val boundsWidth = menubarBounds.useContents { size.width }
    val boundsHeight = menubarBounds.useContents { size.height }
    maskLayer.frame = NSMakeRect(30.0, 0.0, boundsWidth - 30 - 8, boundsHeight)

    textLayer1 = setupTextLayer(isFirstLayer = true)
    textLayer2 = setupTextLayer(isFirstLayer = false)
    maskLayer.addSublayer(textLayer1)
    maskLayer.addSublayer(textLayer2)
  }
}
