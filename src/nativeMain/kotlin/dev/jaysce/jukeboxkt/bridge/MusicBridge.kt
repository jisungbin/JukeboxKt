package dev.jaysce.jukeboxkt.bridge

import ScriptingBridge.SBApplication
import ScriptingBridge.SBObject
import dev.jaysce.jukeboxkt.model.Track
import dev.jaysce.jukeboxkt.util.Constants
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AppKit.NSImage
import platform.AppKit.NSRunningApplication
import platform.Foundation.NSAppleEventDescriptor
import platform.Foundation.NSAppleScript
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSDictionary
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataUsingEncoding
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

class MusicBridge {
  private val bundleId = Constants.AppleMusic.bundleID

  private val artworkCacheDir: String by lazy {
    val cachesDir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
      .firstOrNull() as? String ?: NSTemporaryDirectory()
    val dir = "$cachesDir/dev.jaysce.jukebox.kt/artwork"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    dir
  }

  val isRunning: Boolean
    get() = NSRunningApplication.runningApplicationsWithBundleIdentifier(bundleId).isNotEmpty()

  fun getCurrentTrack(): Track? {
    if (!isRunning) {
      println("[Jukebox] getCurrentTrack: Music not running")
      return null
    }
    val result = executeScript(
      """
            tell application "Music"
                if player state is stopped then return ""
                return name of current track & "|||" & artist of current track & "|||" & album of current track
            end tell
            """.trimIndent(),
    ) ?: return null

    val parts = result.split("|||")
    if (parts.size < 3) {
      println("[Jukebox] getCurrentTrack: unexpected format — $result")
      return null
    }
    val track = Track(title = parts[0], artist = parts[1], album = parts[2])
    println("[Jukebox] getCurrentTrack: ${track.title} — ${track.artist} — ${track.album}")
    return track
  }

  fun getTrackDuration(): Double {
    val duration = executeScript("""tell application "Music" to return duration of current track""")?.toDoubleOrNull() ?: 0.0
    println("[Jukebox] getTrackDuration: $duration")
    return duration
  }

  fun getPlayerPosition(): Double =
    executeScript("""tell application "Music" to return player position""", silent = true)?.toDoubleOrNull() ?: 0.0

  fun isPlaying(): Boolean {
    val playing = executeScriptRaw("""tell application "Music" to return player state is playing""")?.booleanValue ?: false
    println("[Jukebox] isPlaying: $playing")
    return playing
  }

  fun playPause() {
    println("[Jukebox] playPause")
    executeScript("""tell application "Music" to playpause""")
  }

  fun nextTrack() {
    println("[Jukebox] nextTrack")
    executeScript("""tell application "Music" to next track""")
  }

  fun previousTrack() {
    println("[Jukebox] previousTrack")
    executeScript("""tell application "Music" to back track""")
  }

  fun setFavorited(favorited: Boolean) {
    println("[Jukebox] setFavorited: $favorited")
    executeScript("""tell application "Music" to set favorited of current track to $favorited""")
  }

  fun isFavorited(): Boolean {
    val favorited = executeScriptRaw("""tell application "Music" to return favorited of current track""")?.booleanValue ?: false
    println("[Jukebox] isFavorited: $favorited")
    return favorited
  }

  fun getAlbumArtwork(): NSImage? {
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
    println("[Jukebox] getAlbumArtwork: AppleScript path = $path")
    if (!path.isNullOrEmpty()) {
      NSImage(contentsOfFile = path)?.let {
        println("[Jukebox] getAlbumArtwork: loaded from temp file OK")
        return it
      }
      println("[Jukebox] getAlbumArtwork: NSImage(contentsOfFile:) failed for path")
    }

    // Fallback: SBApplication (라이브러리 트랙용)
    println("[Jukebox] getAlbumArtwork: trying SBApplication fallback")
    return getAlbumArtworkViaSB()
  }

  private fun getAlbumArtworkViaSB(): NSImage? {
    val app = SBApplication.applicationWithBundleIdentifier(bundleId) ?: run {
      println("[Jukebox] SB: applicationWithBundleIdentifier returned null")
      return null
    }
    val currentTrack = app.valueForKey("currentTrack") as? SBObject ?: run {
      println("[Jukebox] SB: currentTrack is null or not SBObject")
      return null
    }
    val artworks = currentTrack.valueForKey("artworks") as? NSArray ?: run {
      println("[Jukebox] SB: artworks is null or not NSArray")
      return null
    }
    if (artworks.count.toInt() == 0) {
      println("[Jukebox] SB: artworks count is 0")
      return null
    }
    val artwork = artworks.objectAtIndex(0u) as? SBObject ?: return null
    val dataObj = artwork.valueForKey("data")
    println("[Jukebox] SB: artwork data type = ${dataObj?.let { it::class.simpleName ?: "unknown" } ?: "null"}")

    (dataObj as? NSImage)?.let { println("[Jukebox] SB: artwork loaded as NSImage"); return it }
    (dataObj as? NSAppleEventDescriptor)?.data?.let { println("[Jukebox] SB: artwork loaded via NSAppleEventDescriptor"); return NSImage(data = it) }
    (dataObj as? NSData)?.let { println("[Jukebox] SB: artwork loaded as NSData"); return NSImage(data = it) }
    (artwork.valueForKey("rawData") as? NSData)?.let { println("[Jukebox] SB: artwork loaded via rawData"); return NSImage(data = it) }
    println("[Jukebox] SB: all artwork extraction methods failed")
    return null
  }

  fun getAlbumArtworkWithRetry(
    title: String = "",
    artist: String = "",
    album: String = "",
    onResult: (NSImage?) -> Unit,
  ) {
    // 1. AppleScript + SB (라이브러리 트랙)
    getAlbumArtwork()?.let {
      println("[Jukebox] artworkWithRetry: sync success")
      onResult(it)
      return
    }

    // 2. iTunes Search API (비라이브러리 스트리밍 트랙용)
    println("[Jukebox] artworkWithRetry: sync failed, trying iTunes Search")
    getAlbumArtworkViaITunesSearch(title, artist, album) { image ->
      if (image != null) println("[Jukebox] artworkWithRetry: iTunes Search success")
      else println("[Jukebox] artworkWithRetry: all methods exhausted")
      onResult(image)
    }
  }

  // --- iTunes Search API를 통한 아트워크 (비라이브러리 스트리밍 트랙용, 인증 불필요) ---
  private fun getAlbumArtworkViaITunesSearch(title: String, artist: String, album: String, onResult: (NSImage?) -> Unit) {
    if (title.isBlank() && artist.isBlank()) {
      onResult(null)
      return
    }

    val cacheFile = "$artworkCacheDir/${"$title|||$artist|||$album".hashCode().toUInt().toString(16)}.jpg"
    if (NSFileManager.defaultManager.fileExistsAtPath(cacheFile)) {
      NSImage(contentsOfFile = cacheFile)?.let {
        println("[Jukebox] iTunes Search: disk cache hit for '$title' by '$artist' on '$album'")
        onResult(it)
        return
      }
    }
    println("[Jukebox] iTunes Search: cache miss, fetching '$title' by '$artist' on '$album'")

    val components = NSURLComponents().apply {
      scheme = "https"
      host = "itunes.apple.com"
      path = "/search"
      queryItems = listOf(
        NSURLQueryItem(name = "term", value = listOf(title, artist, album).filter { it.isNotBlank() }.joinToString(" ")),
        NSURLQueryItem(name = "media", value = "music"),
        NSURLQueryItem(name = "entity", value = "musicTrack"),
        NSURLQueryItem(name = "limit", value = "3"),
      )
    }
    val searchUrl = components.URL?.absoluteString ?: run {
      println("[Jukebox] iTunes Search: invalid URL")
      onResult(null)
      return
    }

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0uL)) {
      // 1. Search API 호출
      val json = shellExec("/usr/bin/curl -sf '$searchUrl'")
      if (json.isNullOrEmpty()) {
        println("[Jukebox] iTunes Search: request failed")
        dispatch_async(dispatch_get_main_queue()) { onResult(null) }
        return@dispatch_async
      }

      // 2. JSON 파싱 → 아트워크 URL 추출
      val artworkUrl = parseArtworkUrlFromJson(json)
      if (artworkUrl == null) {
        println("[Jukebox] iTunes Search: no results for '$title' by '$artist' on '$album'")
        dispatch_async(dispatch_get_main_queue()) { onResult(null) }
        return@dispatch_async
      }

      // 3. 아트워크 다운로드 → 디스크 캐시 → NSImage
      shellExec("/usr/bin/curl -sf -o '$cacheFile' '$artworkUrl'")
      val image = NSImage(contentsOfFile = cacheFile)
      if (image != null) {
        println("[Jukebox] iTunes Search: fetched and cached '$title' by '$artist' on '$album'")
      } else {
        println("[Jukebox] iTunes Search: artwork download failed")
      }
      dispatch_async(dispatch_get_main_queue()) { onResult(image) }
    }
  }

  @Suppress("CAST_NEVER_SUCCEEDS")
  private fun parseArtworkUrlFromJson(json: String): String? {
    val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    val dict = try {
      NSJSONSerialization.JSONObjectWithData(data, options = 0u, error = null) as? NSDictionary
    } catch (_: Exception) {
      null
    } ?: return null
    val results = dict.objectForKey("results") as? NSArray ?: return null
    if (results.count.toInt() == 0) return null
    val first = results.objectAtIndex(0u) as? NSDictionary ?: return null
    val artworkUrl100 = first.objectForKey("artworkUrl100") as? String ?: return null
    // 100x100 → 600x600 고해상도
    return artworkUrl100.replace("100x100bb", "600x600bb")
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
        sb.toString().trim().ifEmpty { null }
      }
    } finally {
      pclose(fp)
    }
  }

  private fun executeScript(source: String, silent: Boolean = false): String? {
    val result = executeScriptRaw(source, silent)
    val str = result?.stringValue
    if (!silent) println("[Jukebox] executeScript: result=$str, script=${source.take(60)}")
    return str
  }

  private fun executeScriptRaw(source: String, silent: Boolean = false): NSAppleEventDescriptor? = memScoped {
    val errorInfo = alloc<ObjCObjectVar<Map<Any?, *>?>>()
    val script = NSAppleScript(source = source)
    val result = script.executeAndReturnError(errorInfo.ptr)
    if (!silent) println("[Jukebox] executeScriptRaw: ${if (result != null) "OK" else "FAILED"}, script=${source.take(60)}")
    result
  }
}
