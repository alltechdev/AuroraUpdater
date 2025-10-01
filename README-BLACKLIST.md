# Aurora Updater - Auto-Blacklist Branch

This branch contains enhanced Aurora Updater with **automatic blacklist updates** and improved user experience.

## Features

- **Auto-blacklist updates every 15 seconds** while app is running
- **Automatic UI refresh** when blacklist changes (no manual pull-to-refresh needed)
- **Smart change detection** - only updates when blacklist actually changes
- **Automatic APK building** via GitHub Actions when code changes

## Building the App

### GitHub Actions Auto-Build

The repository automatically builds APKs when you push changes to the `feature/auto-blacklist-updates` branch:

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
git checkout feature/auto-blacklist-updates
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

## Configuring Blacklist URLs

The app fetches blacklist data from a remote JSON source. You can configure different blacklist URLs:

### Default Configuration

The app uses this default GitHub API URL:
```
https://api.github.com/repos/alltechdev/alltech.dev/contents/blacklist.json?ref=main
```

### Setting Up Your Own Blacklist

#### Option 1: GitHub Repository (Recommended)

1. **Create a GitHub repository** with a `blacklist.json` file
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
https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/contents/blacklist.json?ref=main
```

4. **Update the app** by modifying this line in `RemoteBlacklistProvider.kt`:
```kotlin
var remoteBlacklistUrl: String
    get() = Preferences.getString(
        context, 
        Preferences.PREFERENCE_REMOTE_BLACKLIST_URL,
        "https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/contents/blacklist.json?ref=main"
    )
```

### Blacklist JSON Format

Your blacklist JSON must be an array of Android package names:

```json
[
  "com.package.name1",
  "com.package.name2",
  "com.another.package"
]
```

## How Auto-Updates Work

1. **App starts** → Immediate blacklist fetch from configured URL
2. **Every 15 seconds** → Background fetch and comparison
3. **If changes detected** → Update local blacklist + emit event
4. **UpdatesFragment receives event** → Automatically refresh app list
5. **User sees changes** → No manual action required


---

## License

This project maintains the same license as the original Aurora Store.
---

**Happy updating! 🚀**
