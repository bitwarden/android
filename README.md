[![appveyor build](https://ci.appveyor.com/api/projects/status/github/bitwarden/mobile?branch=master&svg=true)](https://ci.appveyor.com/project/bitwarden/mobile)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/bitwarden-mobile/localized.svg)](https://crowdin.com/project/bitwarden-mobile)
[![Join the chat at https://gitter.im/bitwarden/Lobby](https://badges.gitter.im/bitwarden/Lobby.svg)](https://gitter.im/bitwarden/Lobby)

# bitwarden mobile

<a href="https://play.google.com/store/apps/details?id=com.x8bit.bitwarden" target="_blank"><img alt="Get it on Google Play" src="https://imgur.com/YQzmZi9.png" width="153" height="46"></a> <a href="https://itunes.apple.com/us/app/bitwarden-free-password-manager/id1137397744?mt=8" target="_blank"><img src="https://imgur.com/GdGqPMY.png" width="135" height="40"></a> <a href="https://www.amazon.com/dp/B06XMYGPMV" target="_blank"><img src="https://imgur.com/f75uYeM.png" width="132" height="45"></a>

The bitwarden mobile application is written in C# with Xamarin Android, Xamarin iOS, and Xamarin Forms.

<img src="https://i.imgur.com/Ty8wkSO.png" alt="" width="300" height="533" /> <img src="https://i.imgur.com/BYb4gVc.png" alt="" width="300" height="533" />

# Build/Run

**Requirements**

- [Visual Studio w/ Xamarin -or- Xamarin Studio](https://store.xamarin.com/)

By default the app is targeting the production API. If you are running the [Core](https://github.com/bitwarden/core) API locally,
you'll need to switch the app to target your local API. Open `src/App/Utilities/ApiHttpClient.cs` and set `BaseAddress` to your local
API instance (ex. `new Uri("http://localhost:4000")`).

After restoring the nuget packages, you can now build and run the app.

# Contribute

Code contributions are welcome! Visual Studio or Xamarin Studio is required to work on this project. Please commit any pull requests against the `master` branch.
Learn more about how to contribute by reading the [`CONTRIBUTING.md`](CONTRIBUTING.md) file.

Security audits and feedback are welcome. Please open an issue or email us privately if the report is sensitive in nature. You can read our security policy in the [`SECURITY.md`](SECURITY.md) file.
