package dev.jaysce.jukeboxkt.view

import kotlinx.cinterop.CValue
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import platform.AppKit.NSImage
import platform.CoreGraphics.CGSize
import platform.Metal.MTLBufferProtocol
import platform.Metal.MTLClearColorMake
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLComputePipelineStateProtocol
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLSizeMake
import platform.Metal.MTLTextureProtocol
import platform.MetalKit.MTKTextureLoader
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.darwin.NSObject

public class AlbumWarpRenderer(private val mtkView: MTKView) : NSObject(), MTKViewDelegateProtocol {
  private val timeStep = 1.0f / 30.0f
  private var time = 0.0f

  private var device: MTLDeviceProtocol? = null
  private var commandQueue: MTLCommandQueueProtocol? = null
  private var computePipelineState: MTLComputePipelineStateProtocol? = null
  private var timeBuffer: MTLBufferProtocol? = null
  private var inputTexture: MTLTextureProtocol? = null
  private var textureLoader: MTKTextureLoader? = null

  init {
    device = MTLCreateSystemDefaultDevice()
    mtkView.device = device
    mtkView.preferredFramesPerSecond = 30
    mtkView.framebufferOnly = false
    mtkView.clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
    mtkView.delegate = this
    mtkView.setPaused(true)

    commandQueue = device?.newCommandQueue()
    device?.let { textureLoader = MTKTextureLoader(device = it) }
    createComputePipelineState()
    timeBuffer = device?.newBufferWithLength(4u, options = 0u)
  }

  private fun createComputePipelineState() {
    val library = device?.newDefaultLibrary() ?: return
    val function = library.newFunctionWithName("albumWarp") ?: return

    computePipelineState = runCatching {
      device?.newComputePipelineStateWithFunction(function, error = null)
    }
      .getOrNull()
  }

  public fun updateAlbumArt(image: NSImage?) {
    if (image == null) {
      inputTexture = null
      mtkView.setPaused(true)
      return
    }
    val tiffData = image.TIFFRepresentation ?: return

    inputTexture = runCatching {
      textureLoader?.newTextureWithData(tiffData, options = null, error = null)
    }
      .getOrNull()

    if (inputTexture != null) {
      mtkView.setPaused(false)
    }
  }

  public fun setPaused(paused: Boolean) {
    mtkView.setPaused(paused || inputTexture == null)
  }

  override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {}

  override fun drawInMTKView(view: MTKView) {
    val drawable = view.currentDrawable ?: return
    val pipelineState = computePipelineState ?: return
    val input = inputTexture ?: return

    time += timeStep
    timeBuffer?.contents()?.let { it.reinterpret<FloatVar>()[0] = time }

    val commandBuffer = commandQueue?.commandBuffer() ?: return
    val encoder = commandBuffer.computeCommandEncoder() ?: return
    val outputTexture = drawable.texture as MTLTextureProtocol

    encoder.setComputePipelineState(pipelineState)
    encoder.setTexture(input, atIndex = 0u)
    encoder.setTexture(outputTexture, atIndex = 1u)
    encoder.setBuffer(timeBuffer, offset = 0u, atIndex = 0u)

    val w = pipelineState.threadExecutionWidth.toInt()
    val h = (pipelineState.maxTotalThreadsPerThreadgroup / pipelineState.threadExecutionWidth).toInt()
    val threadGroupCount = MTLSizeMake(w.toULong(), h.toULong(), depth = 1uL)
    val threadGroups = MTLSizeMake(
      ((outputTexture.width.toInt() + w - 1) / w).toULong(),
      ((outputTexture.height.toInt() + h - 1) / h).toULong(),
      depth = 1uL,
    )

    encoder.dispatchThreadgroups(threadGroups, threadsPerThreadgroup = threadGroupCount)
    encoder.endEncoding()

    commandBuffer.presentDrawable(drawable)
    commandBuffer.commit()
  }
}
