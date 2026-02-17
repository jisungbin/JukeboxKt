package dev.jaysce.jukeboxkt.util

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSAppleScript

enum class PermissionStatus {
  CLOSED, GRANTED, NOT_PROMPTED, DENIED
}

fun promptUserForConsent(appBundleID: String): PermissionStatus {
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
      PermissionStatus.GRANTED
    } else {
      val errorNumber = (error["NSAppleScriptErrorNumber"] as? Number)?.toInt()
      when (errorNumber) {
        -600 -> PermissionStatus.CLOSED
        -1743 -> PermissionStatus.DENIED
        -1744 -> PermissionStatus.NOT_PROMPTED
        else -> PermissionStatus.DENIED
      }
    }
  }
}
