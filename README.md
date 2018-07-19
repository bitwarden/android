[![appveyor build](https://ci.appveyor.com/api/projects/status/github/bitwarden/mobile?branch=master&svg=true)](https://ci.appveyor.com/project/bitwarden/mobile)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/bitwarden-mobile/localized.svg)](https://crowdin.com/project/bitwarden-mobile)
[![Join the chat at https://gitter.im/bitwarden/Lobby](https://badges.gitter.im/bitwarden/Lobby.svg)](https://gitter.im/bitwarden/Lobby)

# Bitwarden Mobile Application

<a href="https://play.google.com/store/apps/details?id=com.x8bit.bitwarden" target="_blank"><img alt="Get it on Google Play" src="https://imgur.com/YQzmZi9.png" width="153" height="46"></a> <a href="https://itunes.apple.com/us/app/bitwarden-free-password-manager/id1137397744?mt=8" target="_blank"><img src="https://imgur.com/GdGqPMY.png" width="135" height="40"></a>

The Bitwarden mobile application is written in C# with Xamarin Android, Xamarin iOS, UWP, and Xamarin Forms.

<img src="https://raw.githubusercontent.com/bitwarden/brand/master/screenshots/mobile-android.png" alt="" width="300" height="533" /> <img src="https://raw.githubusercontent.com/bitwarden/brand/master/screenshots/mobile-ios.png" alt="" width="300" height="533" />

# Build/Run

**Requirements**

- [Visual Studio](https://store.xamarin.com/)

**API endpoint**

By default the app is targeting the production API. If you are running the [Core](https://github.com/bitwarden/core) API locally,
you'll need to switch the app to target your local instance. Open `src/App/Utilities/ApiHttpClient.cs` and `src/App/Utilities/IdentityHttpClient.cs` and set the `BaseAddress` to your local
API endpoints (ex. `new Uri("http://localhost:5000")`). Alternatively, you can also adjust the environment endpoints from the environment settings page on the home screen of the app (log out).

**Run the app**

After restoring the nuget packages, you can now build and run the app.

# Contribute

Code contributions are welcome! Visual Studio or Xamarin Studio is required to work on this project. Please commit any pull requests against the `master` branch.
Learn more about how to contribute by reading the [`CONTRIBUTING.md`](CONTRIBUTING.md) file.

Security audits and feedback are welcome. Please open an issue or email us privately if the report is sensitive in nature. You can read our security policy in the [`SECURITY.md`](SECURITY.md) file.
