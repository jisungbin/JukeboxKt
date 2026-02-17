package dev.jaysce.jukeboxkt.view

import kotlinx.cinterop.CValue
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLBufferProtocol
import platform.Metal.MTLClearColorMake
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLComputePipelineStateProtocol
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLSizeMake
import platform.Metal.MTLTextureProtocol
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.darwin.NSObject

class MetalRenderer(
  private val functionName: String,
  private val mtkView: MTKView,
) : NSObject(), MTKViewDelegateProtocol {

  private val timeStep = 1.0f / 60.0f
  private var time = 0.0f

  private var device: MTLDeviceProtocol? = null
  private var commandQueue: MTLCommandQueueProtocol? = null
  private var computePipelineState: MTLComputePipelineStateProtocol? = null
  private var timeBuffer: MTLBufferProtocol? = null

  init {
    device = MTLCreateSystemDefaultDevice()
    mtkView.device = device
    mtkView.preferredFramesPerSecond = 60
    mtkView.framebufferOnly = false
    mtkView.clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
    mtkView.drawableSize = mtkView.frame.useContents { CGSizeMake(size.width, size.height) }
    mtkView.delegate = this

    commandQueue = device?.newCommandQueue()
    createComputePipelineState()
    timeBuffer = device?.newBufferWithLength(4u, options = 0u)
  }

  private fun createComputePipelineState() {
    val library = device?.newDefaultLibrary() ?: return
    val function = library.newFunctionWithName(functionName) ?: return
    computePipelineState = try {
      device?.newComputePipelineStateWithFunction(function, error = null)
    } catch (e: Exception) {
      println("Error creating pipeline: $e")
      null
    }
  }

  override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {}

  override fun drawInMTKView(view: MTKView) {
    val drawable = view.currentDrawable ?: return
    val pipelineState = computePipelineState ?: return

    time += timeStep
    timeBuffer?.contents()?.let { it.reinterpret<FloatVar>()[0] = time }

    val commandBuffer = commandQueue?.commandBuffer() ?: return
    val encoder = commandBuffer.computeCommandEncoder() ?: return
    val texture = drawable.texture as MTLTextureProtocol

    encoder.setComputePipelineState(pipelineState)
    encoder.setTexture(texture, atIndex = 0u)
    encoder.setBuffer(timeBuffer, offset = 0u, atIndex = 0u)

    val w = pipelineState.threadExecutionWidth.toInt()
    val h = (pipelineState.maxTotalThreadsPerThreadgroup / pipelineState.threadExecutionWidth).toInt()
    val threadGroupCount = MTLSizeMake(w.toULong(), h.toULong(), 1uL)
    val threadGroups = MTLSizeMake(
      ((texture.width.toInt() + w - 1) / w).toULong(),
      ((texture.height.toInt() + h - 1) / h).toULong(),
      1uL,
    )

    encoder.dispatchThreadgroups(threadGroups, threadsPerThreadgroup = threadGroupCount)
    encoder.endEncoding()
    commandBuffer.presentDrawable(drawable)
    commandBuffer.commit()
  }

  fun setPaused(paused: Boolean) {
    mtkView.setPaused(paused)
  }
}
