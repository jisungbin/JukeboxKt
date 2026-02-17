package dev.jaysce.jukeboxkt.util

import dev.jaysce.jukeboxkt.util.PermissionStatus.CLOSED
import dev.jaysce.jukeboxkt.util.PermissionStatus.DENIED
import dev.jaysce.jukeboxkt.util.PermissionStatus.GRANTED
import dev.jaysce.jukeboxkt.util.PermissionStatus.NOT_PROMPTED
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSAppleScript

public enum class PermissionStatus {
  CLOSED,
  GRANTED,
  NOT_PROMPTED,
  DENIED,
}

public fun promptUserForConsent(appBundleID: String): PermissionStatus {
  val source = """
        tell application id "$appBundleID"
            return name
        end tell
    """.trimIndent()

  return memScoped {
    val errorDict = alloc<ObjCObjectVar<Map<Any?, *>?>>()
    val script = NSAppleScript(source = source)
    script.executeAndReturnError(errorDict.ptr)

    val error = errorDict.value
    if (error == null) {
      GRANTED
    } else {
      when ((error["NSAppleScriptErrorNumber"] as? Number)?.toInt()) {
        -600 -> CLOSED
        -1743 -> DENIED
        -1744 -> NOT_PROMPTED
        else -> DENIED
      }
    }
  }
}
