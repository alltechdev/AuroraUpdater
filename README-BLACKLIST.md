# Aurora Updater - Auto-Blacklist Branch 🚀

This branch contains enhanced Aurora Updater with **automatic blacklist updates** and improved user experience.

## ✨ New Features

- 🔄 **Auto-blacklist updates every 15 seconds** while app is running
- 🚀 **Automatic UI refresh** when blacklist changes (no manual pull-to-refresh needed)
- 🎯 **Smart change detection** - only updates when blacklist actually changes
- 🔧 **Removed blacklist password protection** for streamlined access
- 📡 **Fixed GitHub API integration** with proper URL and base64 decoding
- 🏗️ **Automatic APK building** via GitHub Actions when code changes

## 🏗️ Building the App

### Prerequisites
- **Java 21** (OpenJDK recommended)
- **Android SDK** (API level 34+)
- **Git**

### Local Build

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

### GitHub Actions Auto-Build

The repository automatically builds APKs when you push changes to the `feature/auto-blacklist-updates` branch:

1. **Push changes** to trigger build
2. **Go to Actions tab** in your GitHub repository
3. **Download artifacts** from the completed workflow

**Builds are triggered by:**
- ✅ Changes to `.kt`, `.java`, `.xml`, `.gradle` files
- ❌ **NOT triggered by** README, docs, or other non-code changes

## 🔗 Configuring Blacklist URLs

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

#### Option 2: Direct JSON URL

For direct JSON hosting (not GitHub API):

1. **Host your JSON file** on any web server
2. **Update the parsing logic** in `RemoteBlacklistProvider.kt`:
```kotlin
val remoteBlacklist = try {
    // Check if this is a GitHub API response or direct JSON
    if (remoteBlacklistUrl.contains("api.github.com")) {
        // GitHub API response parsing (existing code)
        val apiResponse = json.parseToJsonElement(responseBody).jsonObject
        val contentEncoded = apiResponse["content"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("No content field in GitHub API response")
        
        val decodedContent = String(Base64.getDecoder().decode(contentEncoded.replace("\n", "")))
        json.decodeFromString<List<String>>(decodedContent).toMutableSet()
    } else {
        // Direct JSON response - ADD THIS FOR YOUR CUSTOM URL
        json.decodeFromString<List<String>>(responseBody).toMutableSet()
    }
} catch (e: Exception) {
    Log.e(TAG, "Failed to parse remote blacklist JSON", e)
    return@withContext false
}
```

#### Option 3: Runtime Configuration

You could also add a settings screen to let users configure their own blacklist URL:

1. **Add a preference** in `preferences_settings.xml`
2. **Create a settings handler** in `SettingsFragment.kt`
3. **Update the URL** through `Preferences.putString()`

### Blacklist JSON Format

Your blacklist JSON must be an array of Android package names:

```json
[
  "com.package.name1",
  "com.package.name2",
  "com.another.package"
]
```

**Package Name Examples:**
- `com.facebook.katana` (Facebook)
- `com.instagram.android` (Instagram)
- `com.whatsapp` (WhatsApp)
- `com.tiktok.musically` (TikTok)

## 🔄 How Auto-Updates Work

1. **App starts** → Immediate blacklist fetch from configured URL
2. **Every 15 seconds** → Background fetch and comparison
3. **If changes detected** → Update local blacklist + emit event
4. **UpdatesFragment receives event** → Automatically refresh app list
5. **User sees changes** → No manual action required

## 🐛 Debugging

### Enable Debug Logging

The app includes comprehensive logging. To see blacklist activity:

```bash
adb logcat | grep -E "(RemoteBlacklistProvider|BlacklistProvider)"
```

**Debug logs show:**
- ✅ Successful updates with entry counts
- 🔄 "Unchanged, skipping update" when no changes
- ❌ Network errors and parsing failures
- 📝 First 5 blacklist entries for verification

### Common Issues

**Updates not working?**
- Check internet connection
- Verify blacklist URL is accessible
- Check logs for parsing errors

**UI not refreshing?**
- Verify `BusEvent.BlacklistUpdated` is being emitted
- Check `UpdatesFragment` event listener is active

## 📦 Distribution

### Debug vs Release Builds

- **Debug APK:** For testing, includes logging, larger file size
- **Release APK:** Optimized for distribution, smaller size, obfuscated

### GitHub Actions Artifacts

Each successful build creates downloadable artifacts:
- `aurora-updater-debug-<commit-sha>`
- `aurora-updater-release-<commit-sha>`

Artifacts are kept for **30 days** and include the full commit SHA for tracking.

## 🔧 Technical Details

### Architecture Changes

- **AuroraApp.kt:** Handles global 15-second update timer
- **RemoteBlacklistProvider.kt:** GitHub API integration with base64 decoding
- **BusEvent.kt:** Added `BlacklistUpdated` event for UI notifications
- **UpdatesFragment.kt:** Automatic refresh on blacklist changes
- **Settings:** Removed blacklist password UI components

### Performance Optimizations

- ⚡ **Change detection** prevents unnecessary UI updates
- 🎯 **Background threading** for network requests
- 💾 **Efficient caching** with timestamp tracking
- 🔄 **Event-driven UI** updates only when needed

---

## 📄 License

This project maintains the same license as the original Aurora Store.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch from `feature/auto-blacklist-updates`
3. Make your changes
4. Test with both debug and release builds
5. Submit a pull request

---

**Happy updating! 🚀**