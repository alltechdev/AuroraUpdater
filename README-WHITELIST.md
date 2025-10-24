# Aurora Updater - Auto-Whitelist Branch

This branch contains enhanced Aurora Updater with **automatic whitelist updates** and improved user experience.

## What is Whitelist Mode?

**IMPORTANT:** This is a WHITELIST system, not a blacklist:
- **Whitelist**: Shows ONLY apps that are in your JSON list
- **Blacklist** (original): Hides apps that are in the list, shows everything else

If your whitelist.json is empty or unreachable, NO APPS will be shown in the store.

## Features

- **Auto-whitelist updates every 15 seconds** while app is running
- **Automatic UI refresh** when whitelist changes (no manual pull-to-refresh needed)
- **Smart change detection** - only updates when whitelist actually changes
- **Automatic APK building** via GitHub Actions when code changes

## Building the App

### GitHub Actions Auto-Build

The repository automatically builds APKs when you push changes to the `feature/auto-whitelist-updates` branch:

1. **Push changes** to trigger build
2. **Go to Actions tab** in your GitHub repository
3. **Download artifacts** from the completed workflow

**Builds are triggered by:**
- Changes to `.kt`, `.java`, `.xml`, `.gradle` files
- **NOT triggered by** README, docs, or other non-code changes

### Local Build

### Prerequisites
- **Java 21** (OpenJDK recommended)
- **Android SDK** (API level 34+)
- **Git**

1. **Clone and checkout the branch:**
```bash
git clone <your-repo-url>
cd AuroraUpdater
git checkout feature/auto-whitelist-updates
```

2. **Set Java 21:**
```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

3. **Build APKs:**
```bash
# Debug build (for testing)
./gradlew assembleVanillaDebug

# Release build (for distribution)
./gradlew assembleVanillaRelease
```

4. **Find built APKs:**
- Debug: `app/build/outputs/apk/vanilla/debug/`
- Release: `app/build/outputs/apk/vanilla/release/`

## Configuring Whitelist URLs

The app fetches whitelist data from a remote JSON source. You can configure different whitelist URLs:

### Default Configuration

The app uses this default GitHub API URL:
```
https://api.github.com/repos/alltechdev/alltech.dev/contents/whitelist.json?ref=main
```

### Setting Up Your Own Whitelist

#### Option 1: GitHub Repository (Recommended)

1. **Create a GitHub repository** with a `whitelist.json` file
2. **Format your JSON** as an array of package names:
```json
[
  "com.example.unwantedapp1",
  "com.example.unwantedapp2",
  "com.badapp.malware"
]
```

3. **Get the GitHub API URL:**
```
https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/contents/whitelist.json?ref=main
```

4. **Update the app** by modifying this line in `RemoteWhitelistProvider.kt`:
```kotlin
var remoteWhitelistUrl: String
    get() = Preferences.getString(
        context, 
        Preferences.PREFERENCE_REMOTE_WHITELIST_URL,
        "https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/contents/whitelist.json?ref=main"
    )
```

### Whitelist JSON Format

Your whitelist JSON must be an array of Android package names:

```json
[
  "com.package.name1",
  "com.package.name2",
  "com.another.package"
]
```

## How Auto-Updates Work

1. **App starts** → Immediate whitelist fetch from configured URL
2. **Every 15 seconds** → Background fetch and comparison
3. **If changes detected** → Update local whitelist + emit event
4. **UpdatesFragment receives event** → Automatically refresh app list
5. **User sees changes** → No manual action required


---

## License

This project maintains the same license as the original Aurora Store.
---

**Happy updating!**
