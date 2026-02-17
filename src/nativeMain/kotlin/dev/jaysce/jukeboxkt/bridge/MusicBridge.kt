package dev.jaysce.jukeboxkt.bridge

import ScriptingBridge.SBApplication
import ScriptingBridge.SBObject
import dev.jaysce.jukeboxkt.model.Track
import dev.jaysce.jukeboxkt.util.Constants
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.AppKit.NSImage
import platform.AppKit.NSRunningApplication
import platform.Foundation.NSAppleEventDescriptor
import platform.Foundation.NSAppleScript
import platform.Foundation.NSArray
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSFileManager
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataUsingEncoding
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

public class MusicBridge {
  private val bundleId = Constants.AppleMusic.bundleID

  private val artworkCacheDir: String by lazy {
    val cachesDir =
      NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, expandTilde = true)
        .firstOrNull() as? String
        ?: NSTemporaryDirectory()
    val dir = "$cachesDir/dev.jaysce.jukeboxkt/artwork"

    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    dir
  }

  public val isRunning: Boolean
    get() = NSRunningApplication.runningApplicationsWithBundleIdentifier(bundleId).isNotEmpty()

  public fun getCurrentTrack(): Track? {
    if (!isRunning) return null
    val result = executeScript(
      """
            tell application "Music"
                if player state is stopped then return ""
                return name of current track & "|||" & artist of current track & "|||" & album of current track
            end tell
            """.trimIndent(),
    ) ?: return null

    val parts = result.split("|||")
    if (parts.size < 3) return null

    return Track(title = parts[0], artist = parts[1], album = parts[2])
  }

  public fun getTrackDuration(): Double =
    executeScript("""tell application "Music" to return duration of current track""")?.toDoubleOrNull() ?: 0.0

  public fun getPlayerPosition(): Double =
    executeScript("""tell application "Music" to return player position""")?.toDoubleOrNull() ?: 0.0

  public fun isPlaying(): Boolean =
    executeScriptRaw("""tell application "Music" to return player state is playing""").booleanValue

  public fun playPause() {
    executeScript("""tell application "Music" to playpause""")
  }

  public fun nextTrack() {
    executeScript("""tell application "Music" to next track""")
  }

  public fun previousTrack() {
    executeScript("""tell application "Music" to back track""")
  }

  public fun setFavorited(favorited: Boolean) {
    executeScript("""tell application "Music" to set favorited of current track to $favorited""")
  }

  public fun isFavorited(): Boolean =
    executeScriptRaw("""tell application "Music" to return favorited of current track""").booleanValue

  public fun getAlbumArtwork(): NSImage? {
    // Primary: AppleScript raw data → temp file → NSImage (비라이브러리 트랙에서도 동작)
    val path = executeScript(
      """
            tell application "Music"
                if player state is stopped then return ""
                if (count of artworks of current track) = 0 then return ""
                tell artwork 1 of current track
                    set artData to raw data
                    if format is JPEG picture then
                        set ext to ".jpg"
                    else
                        set ext to ".png"
                    end if
                end tell
                set filePath to (path to temporary items as text) & "jukebox_artwork" & ext
                set fileRef to open for access file filePath with write permission
                set eof fileRef to 0
                write artData to fileRef
                close access fileRef
                return POSIX path of filePath
            end tell
            """.trimIndent(),
    )
    if (!path.isNullOrEmpty()) {
      return NSImage(contentsOfFile = path)
    }

    // Fallback: SBApplication (라이브러리 트랙용)
    return getAlbumArtworkViaSB()
  }

  private fun getAlbumArtworkViaSB(): NSImage? {
    val app = SBApplication.applicationWithBundleIdentifier(bundleId) ?: return null
    val currentTrack = app.valueForKey("currentTrack") as? SBObject ?: return null

    val artworks = currentTrack.valueForKey("artworks") as? NSArray ?: return null
    if (artworks.count.toInt() == 0) return null
    val artwork = artworks.objectAtIndex(0u) as? SBObject ?: return null

    when (val dataObj = artwork.valueForKey("data")) {
      is NSImage -> return dataObj
      is NSAppleEventDescriptor -> return NSImage(data = dataObj.data)
      is NSData -> return NSImage(data = dataObj)
    }

    val rawData = artwork.valueForKey("rawData")
    if (rawData is NSData) return NSImage(data = rawData)

    return null
  }

  public fun getAlbumArtworkWithRetry(
    title: String = "",
    artist: String = "",
    album: String = "",
    onResult: (NSImage?) -> Unit,
  ) {
    // 1. AppleScript + SB (라이브러리 트랙)
    val artwork = getAlbumArtwork()
    if (artwork != null) {
      onResult(artwork)
      return
    }

    // 2. iTunes Search API (비라이브러리 스트리밍 트랙용)
    getAlbumArtworkViaITunesSearch(title, artist, album, onResult)
  }

  private fun getAlbumArtworkViaITunesSearch(
    title: String,
    artist: String,
    album: String,
    onResult: (NSImage?) -> Unit,
  ) {
    if (title.isBlank() && artist.isBlank()) {
      onResult(null)
      return
    }

    val cacheFile = "$artworkCacheDir/${"$title|||$artist|||$album".hashCode().toUInt().toString(16)}.jpg"
    if (NSFileManager.defaultManager.fileExistsAtPath(cacheFile)) {
      onResult(NSImage(contentsOfFile = cacheFile))
      return
    }

    val components = NSURLComponents().apply {
      scheme = "https"
      host = "itunes.apple.com"
      path = "/search"
      queryItems = listOf(
        NSURLQueryItem(name = "term", value = listOf(title, artist, album).filter(String::isNotBlank).joinToString(" ")),
        NSURLQueryItem(name = "media", value = "music"),
        NSURLQueryItem(name = "entity", value = "musicTrack"),
        NSURLQueryItem(name = "limit", value = "3"),
      )
    }
    val searchUrl = components.URL?.absoluteString ?: run {
      onResult(null)
      return
    }

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), flags = 0uL)) {
      val json = shellExec("/usr/bin/curl -sf '$searchUrl'")
      if (json.isNullOrEmpty()) {
        dispatch_async(dispatch_get_main_queue()) { onResult(null) }
        return@dispatch_async
      }

      val artworkUrl = parseArtworkUrlFromJson(json)
      if (artworkUrl == null) {
        dispatch_async(dispatch_get_main_queue()) { onResult(null) }
        return@dispatch_async
      }

      shellExec("/usr/bin/curl -sf -o '$cacheFile' '$artworkUrl'")

      val image = NSImage(contentsOfFile = cacheFile)
      dispatch_async(dispatch_get_main_queue()) { onResult(image) }
    }
  }

  @Suppress("CAST_NEVER_SUCCEEDS")
  private fun parseArtworkUrlFromJson(json: String): String? {
    val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    val dict = runCatching {
      NSJSONSerialization.JSONObjectWithData(data, options = 0u, error = null) as NSDictionary
    }
      .getOrElse { return null }

    val results = dict.objectForKey("results") as? NSArray ?: return null
    if (results.count.toInt() == 0) return null

    val first = results.objectAtIndex(0u) as? NSDictionary ?: return null
    val artworkUrl = first.objectForKey("artworkUrl100") as? String ?: return null

    // 100x100 → 600x600 고해상도
    return artworkUrl.replace("100x100bb", "600x600bb")
  }

  private fun shellExec(command: String): String? {
    val fp = popen(command, "r") ?: return null
    try {
      return memScoped {
        val buf = allocArray<ByteVar>(4096)
        val sb = StringBuilder()
        while (fgets(buf, 4096, fp) != null) {
          sb.append(buf.toKString())
        }
        sb.toString().trim().takeIf(String::isNotEmpty)
      }
    } finally {
      pclose(fp)
    }
  }

  private fun executeScript(source: String): String? = executeScriptRaw(source).stringValue

  private fun executeScriptRaw(source: String): NSAppleEventDescriptor =
    memScoped {
      val errorInfo = alloc<ObjCObjectVar<Map<Any?, *>?>>()
      val script = NSAppleScript(source = source)
      script.executeAndReturnError(errorInfo.ptr)
    }
}
