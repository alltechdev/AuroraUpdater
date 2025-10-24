# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aurora Updater is a simplified Aurora Store fork focused on app updates only. It's an Android application written in Kotlin with Jetpack Compose UI, using modern Android development practices.

## Build Commands

### Gradle Wrapper
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing.properties)
./gradlew assembleRelease

# Build nightly variant
./gradlew assembleNightly

# Install debug build to connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Lint code
./gradlew lint

# Format code with ktlint
./gradlew ktlintFormat
```

### Build Variants
- `vanilla` (default): Standard version
- `huawei`: Huawei device compatibility version
- `preload`: For preloaded/system installations

### Build Types
- `debug`: Development builds with AOSP signing
- `release`: Production builds (requires signing.properties)
- `nightly`: Development builds with commit hash

## Architecture

### Package Structure
- `com.aurora.store`: Main application package
  - `compose/`: Jetpack Compose UI components
  - `data/`: Data layer (repositories, database, network)
  - `view/`: Views and UI logic
  - `viewmodel/`: ViewModels for MVVM architecture
  - `util/`: Utility classes
  - `module/`: Dependency injection modules

### Key Technologies
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM with Hilt dependency injection
- **Database**: Room with KSP processing
- **Networking**: OkHttp with custom GPlayApi integration
- **Async**: Kotlin Coroutines and WorkManager
- **Image Loading**: Coil
- **Testing**: JUnit, Truth, Espresso, Hilt testing

### Core Components
- **MainActivity**: Main entry point with navigation
- **AuroraApp**: Application class with Hilt setup
- **ComposeActivity**: Base activity for Compose screens
- **GPlayApi Integration**: Custom Play Store API wrapper
- **Spoof Manager**: Authentication fallback system
- **Blacklist Manager**: Password-protected app management

### Dependencies
- Uses Gradle version catalogs for dependency management
- Hilt for dependency injection
- Room for local database
- Retrofit/OkHttp for networking
- Epoxy for complex list views (mixed with Compose)

## Development Notes

### Signing
- Debug builds use AOSP test key
- Release builds require `signing.properties` file with:
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
  - `STORE_FILE`
  - `KEY_PASSWORD`

### Code Style
- Follows official Kotlin code style
- Ktlint for code formatting
- Proguard enabled for release builds

### Special Features
- Anonymous login with Spoof Manager fallback
- Password-protected Blacklist Manager
- Disabled Google Play browsing (updates-only focus)
- Support for multiple device types (vanilla, huawei, preload)