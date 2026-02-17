# Jukebox.kt

> [Jukebox](https://github.com/Jaysce/Jukebox)를 **Kotlin/Native**로 재작성한 포크입니다.

macOS 메뉴 바에서 Apple Music의 재생 정보를 표시하고 제어하는 앱입니다.

## Original vs Jukebox.kt

| | Original (Jukebox) | Jukebox.kt |
|---|---|---|
| Language | Swift (SwiftUI + AppKit) | **Kotlin/Native** (AppKit cinterop) |
| Music Apps | Apple Music + Spotify | Apple Music only |
| Auto Update | Sparkle | Removed (personal use) |
| Launch at Login | LaunchAtLogin lib | SMAppService (direct) |
| Min macOS | 11 Big Sur | **13 Ventura** |
| External Deps | Sparkle, LaunchAtLogin | **None** |

## Features

- Menu bar에 재생 중인 곡 표시 (마퀴 텍스트 + 바 애니메이션)
- 팝오버로 앨범아트, 트랙정보, 재생 컨트롤 제공
- 좋아요, 이전/다음 곡, 재생/일시정지
- Metal 셰이더 기반 온보딩 화면
- 비주얼라이저 스타일 변경 (None / Artwork)
- 로그인 시 자동 실행 (SMAppService)
- 다크/라이트 모드 자동 대응

## Build

Requires: macOS 13+, Kotlin 2.3.20-Beta2, Gradle 9.4.0-rc-1

### Debug (개발용)

```bash
./gradlew linkDebugExecutableNative
./build/bin/native/debugExecutable/Jukebox.kexe
```

### Release (실사용)

```bash
./gradlew packageApp
```

`build/Jukebox.app` 번들이 생성됩니다. 이 태스크는 내부적으로 다음을 수행합니다:

1. `linkReleaseExecutableNative` — 최적화된 릴리스 바이너리 빌드
2. `compileMetalShaders` — Metal 셰이더 컴파일 (Gradient + BaseWarp → default.metallib)
3. `.app/Contents` 번들 구성 (실행 파일, Info.plist, 아이콘, metallib)
4. `codesign` — entitlements 포함 ad-hoc 서명

### 설치

```bash
cp -r build/Jukebox.app /Applications/
open /Applications/Jukebox.app
```

> Apple Events 권한 요청이 표시되면 허용해야 Apple Music 제어가 가능합니다.

### Test

```bash
./gradlew nativeTest
```

## Project Structure

```
src/nativeMain/kotlin/dev/jaysce/jukebox/
├── Main.kt                        # NSApplication entry point
├── app/AppDelegate.kt             # Status bar, popover, onboarding
├── model/
│   ├── Track.kt                   # Immutable data class
│   └── VisualizerStyle.kt         # Background style enum
├── viewmodel/ContentViewModel.kt  # Business logic (callback pattern)
├── view/
│   ├── ContentPopoverView.kt      # Main popover (AppKit)
│   ├── StatusBarAnimation.kt      # Bar animation (CALayer)
│   ├── MenuMarqueeText.kt         # Marquee text (CATextLayer)
│   ├── MetalView.kt               # Metal renderer (MTKView)
│   ├── AboutMenuItemView.kt       # About menu item
│   ├── PreferencesContentView.kt  # Preferences
│   └── OnboardingContentView.kt   # Onboarding
├── window/
│   ├── PreferencesWindow.kt       # NSWindow
│   └── OnboardingWindow.kt        # NSWindow (floating)
├── bridge/MusicBridge.kt          # Apple Music (NSAppleScript + SBApplication)
└── util/
    ├── Constants.kt               # App constants
    ├── Extensions.kt              # String/NSImage extensions + helpers
    └── Permissions.kt             # Apple Events permission check
```

## Architecture

- **ScriptingBridge Hybrid**: 텍스트/숫자 속성은 `NSAppleScript`, 앨범 아트워크(바이너리)는 `SBApplication.valueForKey()` 체인
- **UI**: SwiftUI 대신 AppKit 뷰를 Kotlin/Native cinterop으로 직접 사용
- **State**: Combine/Flow 대신 callback 리스너 패턴
- **No External Dependencies**: 모든 기능을 `platform.*` cinterop으로 구현

## Attributions

- Original project by [Jaysce](https://github.com/Jaysce/Jukebox)
- BaseWarp shader by [trinketMage](https://www.shadertoy.com/view/tdG3Rd)
