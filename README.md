# Aurora Updater

Aurora Updater is a simplified version of Aurora Store focused solely on updating your existing apps. It provides a clean interface to view and manage app updates without the ability to browse or download new applications.

To use Aurora Updater, log in using Google Play account when you first open and configure Aurora Updater.

Unlike a traditional app store, Aurora Updater does not own, license or distribute any apps. All app updates and content are directly accessed from Google Play. 

*_Please note that Aurora Updater does not have any approval, sponsorship or authorization from Google, Google Play, any apps updated through Aurora Updater or any app developers; neither does Aurora Updater have any affiliation, cooperation or connection with them._*

## Features

- FOSS: Has GPLv3 licence
- Clean, simplified interface focused on app updates only
- Beautiful design: Built upon latest Material 3 guidelines
- Account login: You can login with either personal or an anonymous account
- Updates blacklisting: Ignore updates for specific apps
- No app browsing or search functionality - updates only 

## Limitations

- The underlying API used is reversed engineered from the Google Play Store, changes on side may break it.
- Updates only - cannot browse or download new apps
- Cannot update paid apps
- Cannot update apps/games with [Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery)
- Some features not available if logged in as Anonymous
- Token dispenser server is not super reliable, downtimes are expected

## Downloads

Download the latest APK from [GitHub Releases](https://github.com/alltechdev/AuroraUpdater/releases)

## Certificate Fingerprints

- SHA1: 94:42:75:D7:59:8B:C0:3E:48:85:06:06:42:25:A7:19:90:A2:22:02
- SHA256: 4C:62:61:57:AD:02:BD:A3:40:1A:72:63:55:5F:68:A7:96:63:FC:3E:13:A4:D4:36:9A:12:57:09:41:AA:28:0F

## Support

Aurora Store v4 is still in on-going development! Bugs are to be expected! Any bug reports are appreciated.
Please visit [Aurora Wiki](https://gitlab.com/AuroraOSS/AuroraStore/-/wikis/home) for FAQs.

- [Telegram](https://t.me/AuroraSupport)
- [XDA Developers](https://forum.xda-developers.com/t/app-5-0-aurora-store-open-source-google-play-client.3739733/)

## Permissions

- `android.permission.INTERNET` to download and install/update apps from the Google Play servers
- `android.permission.ACCESS_NETWORK_STATE` to check internet availability
- `android.permission.FOREGROUND_SERVICE` to download apps without interruption
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC` to download apps without interruption
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to auto-update apps without interruption (optional)
- `android.permission.MANAGE_EXTERNAL_STORAGE` to access the OBB directory to download APK expansion files for games or large apps
- `android.permission.READ_EXTERNAL_STORAGE` to access the OBB directory to download APK expansion files for games or large apps
- `android.permission.WRITE_EXTERNAL_STORAGE` to access the OBB directory to download APK expansion files for games or large apps
- `android.permission.QUERY_ALL_PACKAGES` to check updates for all installed apps
- `android.permission.REQUEST_INSTALL_PACKAGES` to install and update apps
- `android.permission.REQUEST_DELETE_PACKAGES` to uninstall apps
- `android.permission.ENFORCE_UPDATE_OWNERSHIP` to silently update apps
- `android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION` to silently update apps
- `android.permission.POST_NOTIFICATIONS` to notify user about ongoing downloads, available updates, and errors (optional)
- `android.permission.USE_CREDENTIALS` to allow users to sign into their personal Google account via microG

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png" height="400">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png" height="400">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png" height="400">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png" height="400">

## Translations

Don't see your preferred language? Click on the widget below to help translate Aurora Store!

<a href="https://hosted.weblate.org/engage/aurora-store/">
  <img src="https://hosted.weblate.org/widgets/aurora-store/-/287x66-grey.png" alt="Translation status" />
</a>

## Donations

You can support Aurora Store's development financially via options below. For more options, checkout the **About** page within the Aurora Store.

[![Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/whyorean)
<a href="https://www.paypal.com/paypalme/AuroraDev">
  <img src="https://www.paypalobjects.com/webstatic/mktg/logo/AM_mc_vs_dc_ae.jpg" height="45" alt="PayPal">
</a>

## Project references

Aurora Store is based on these projects

- [YalpStore](https://github.com/yeriomin/YalpStore)
- [AppCrawler](https://github.com/Akdeniz/google-play-crawler)
- [Raccoon](https://github.com/onyxbits/raccoon4)
- [SAI](https://github.com/Aefyr/SAI)
