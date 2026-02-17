<p align="center">
  <img src="art/icon.png" alt="Jukebox.kt" width="128" />
</p>

<h1 align="center">Jukebox.kt</h1>

<p align="center">
  A macOS menu bar app that displays and controls Apple Music playback.
</p>

<p align="center">
  <img src="art/demo.gif" alt="Demo" width="320" />
</p>

A fork of <a href="https://github.com/Jaysce/Jukebox">Jukebox</a>, rewritten in <b>Kotlin/Native</b>.

## Features

- Marquee (scrolling) text in the menu bar showing the current track title and artist
- Equalizer bar animation indicating playback state
- Popover with album art, track info, and elapsed time
- Metal shader background effect based on album art colors
- Favorite, previous/next track, and play/pause controls
- Launch at login
- Automatic dark/light mode support

## Build & Install

```bash
./gradlew packageApp

cp -r build/Jukebox.app /Applications/
open /Applications/Jukebox.app
```

On first launch, you must allow the Apple Events permission prompt to enable Apple Music control.
