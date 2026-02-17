plugins {
  kotlin("multiplatform") version "2.3.20-Beta2"
}

repositories {
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

val appName = "Jukebox"

kotlin {
  explicitApi()

  macosArm64("native") {
    binaries {
      executable {
        entryPoint = "dev.jaysce.jukeboxkt.main"
        linkerOpts(
          "-framework", "AppKit",
          "-framework", "ScriptingBridge",
          "-framework", "QuartzCore",
          "-framework", "ServiceManagement",
          "-F/System/Library/PrivateFrameworks",
        )
      }
    }
    compilations["main"].cinterops {
      val ScriptingBridge by creating
      val DistributedNotificationHelper by creating
    }
  }

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    optIn.addAll(
      "kotlinx.cinterop.ExperimentalForeignApi",
      "kotlinx.cinterop.BetaInteropApi",
    )
  }

  sourceSets {
    val nativeMain by getting
    val nativeTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

// App bundle packaging
abstract class PackageAppTask @Inject constructor(
  private val execOps: ExecOperations,
  private val fsOps: FileSystemOperations,
) : DefaultTask() {
  @get:Input
  abstract var appName: String

  @TaskAction
  fun execute() {
    val buildDir = project.layout.buildDirectory.get().asFile
    val appBundle = buildDir.resolve("$appName.app/Contents")
    val macosDir = appBundle.resolve("MacOS").also { it.mkdirs() }
    val resourcesDir = appBundle.resolve("Resources").also { it.mkdirs() }

    // Copy executable
    fsOps.copy {
      from(buildDir.resolve("bin/native/releaseExecutable/$appName.kexe"))
      into(macosDir)
      rename { appName }
    }

    // Set executable permissions
    execOps.exec { commandLine("chmod", "+x", macosDir.resolve(appName).absolutePath) }

    // Copy Info.plist
    fsOps.copy {
      from(project.file("packaging/Info.plist"))
      into(appBundle)
    }

    // Copy AppIcon if exists
    val iconFile = project.file("packaging/AppIcon.icns")
    if (iconFile.exists()) {
      fsOps.copy {
        from(iconFile)
        into(resourcesDir)
      }
    }

    // Codesign with entitlements
    val entitlements = project.file("packaging/Jukebox.entitlements")
    if (entitlements.exists()) {
      execOps.exec {
        commandLine(
          "codesign", "--force", "--sign", "-",
          "--entitlements", entitlements.absolutePath,
          appBundle.parentFile.absolutePath,
        )
      }
    }
  }
}

tasks.register<PackageAppTask>("packageApp") {
  dependsOn("linkReleaseExecutableNative")
  appName = "Jukebox"
}
