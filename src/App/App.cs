using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Xamarin.Forms;
using System.Diagnostics;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using Bit.App.Controls;
using Plugin.Connectivity.Abstractions;
using System.Net;
using Acr.UserDialogs;
using PushNotification.Plugin.Abstractions;

namespace Bit.App
{
    public class App : Application
    {
        private readonly IDatabaseService _databaseService;
        private readonly IConnectivity _connectivity;
        private readonly IUserDialogs _userDialogs;
        private readonly ISyncService _syncService;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;
        private readonly IPushNotification _pushNotification;

        public App(
            IAuthService authService,
            IConnectivity connectivity,
            IUserDialogs userDialogs,
            IDatabaseService databaseService,
            ISyncService syncService,
            IFingerprint fingerprint,
            ISettings settings,
            IPushNotification pushNotification)
        {
            _databaseService = databaseService;
            _connectivity = connectivity;
            _userDialogs = userDialogs;
            _syncService = syncService;
            _authService = authService;
            _fingerprint = fingerprint;
            _settings = settings;
            _pushNotification = pushNotification;

            SetStyles();

            if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new HomePage();
            }

            MessagingCenter.Subscribe<Application, bool>(Current, "Resumed", async (sender, args) =>
            {
                await CheckLockAsync(args);
                await Task.Run(() => IncrementalSyncAsync()).ConfigureAwait(false);
            });

            MessagingCenter.Subscribe<Application, bool>(Current, "Lock", async (sender, args) =>
            {
                await CheckLockAsync(args);
            });

            MessagingCenter.Subscribe<Application, string>(Current, "Logout", (sender, args) =>
            {
                Logout(args);
            });
        }

        protected async override void OnStart()
        {
            // Handle when your app starts
            await CheckLockAsync(false);
            _databaseService.CreateTables();
            await Task.Run(() => FullSyncAsync()).ConfigureAwait(false);

            Debug.WriteLine("OnStart");
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
            Debug.WriteLine("OnSleep");

            if(Device.OS == TargetPlatform.Android)
            {
                _settings.AddOrUpdateValue(Constants.SettingLastBackgroundedDate, DateTime.UtcNow);
            }
        }

        protected async override void OnResume()
        {
            // Handle when your app resumes
            Debug.WriteLine("OnResume");

            if(Device.OS == TargetPlatform.Android)
            {
                await CheckLockAsync(false);
            }

            var lockPinPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockPinPage;
            if(lockPinPage != null)
            {
                lockPinPage.PinControl.Entry.Focus();
            }
        }

        private async Task IncrementalSyncAsync()
        {
            if(_connectivity.IsConnected)
            {
                var attempt = 0;
                do
                {
                    try
                    {
                        await _syncService.IncrementalSyncAsync();
                        break;
                    }
                    catch(WebException)
                    {
                        Debug.WriteLine("Failed to incremental sync.");
                        if(attempt >= 1)
                        {
                            break;
                        }
                        else
                        {
                            await Task.Delay(1000);
                        }
                        attempt++;
                    }
                } while(attempt <= 1);
            }
            else
            {
                Debug.WriteLine("Not connected.");
            }
        }

        private async Task FullSyncAsync()
        {
            if(_connectivity.IsConnected)
            {
                var attempt = 0;
                do
                {
                    try
                    {
                        await _syncService.FullSyncAsync();
                        break;
                    }
                    catch(WebException)
                    {
                        Debug.WriteLine("Failed to full sync.");
                        if(attempt >= 1)
                        {
                            break;
                        }
                        else
                        {
                            await Task.Delay(1000);
                        }
                        attempt++;
                    }
                } while(attempt <= 1);
            }
        }

        private void Logout(string logoutMessage)
        {
            _authService.LogOut();
            _pushNotification.Unregister();
            Current.MainPage = new HomePage();
            if(!string.IsNullOrWhiteSpace(logoutMessage))
            {
                _userDialogs.WarnToast("Logged out", logoutMessage);
            }
        }

        private async Task CheckLockAsync(bool forceLock)
        {
            // Only lock if they are logged in
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            // Are we forcing a lock? (i.e. clicking a button to lock the app manually, immediately)
            if(!forceLock && !_settings.GetValueOrDefault(Constants.SettingLocked, false))
            {
                // Lock seconds tells if if they want to lock the app or not
                var lockSeconds = _settings.GetValueOrDefault<int?>(Constants.SettingLockSeconds);
                if(!lockSeconds.HasValue)
                {
                    return;
                }

                // Has it been longer than lockSeconds since the last time the app was backgrounded?
                var now = DateTime.UtcNow;
                var lastBackground = _settings.GetValueOrDefault(Constants.SettingLastBackgroundedDate, now.AddYears(-1));
                if((now - lastBackground).TotalSeconds < lockSeconds.Value)
                {
                    return;
                }
            }

            // What method are we using to unlock?
            var fingerprintUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingFingerprintUnlockOn);
            var pinUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingPinUnlockOn);
            var currentPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
            if(fingerprintUnlock && _fingerprint.IsAvailable)
            {
                if((currentPage?.CurrentPage as LockFingerprintPage) == null)
                {
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockFingerprintPage(!forceLock)), false);
                }
            }
            else if(pinUnlock && !string.IsNullOrWhiteSpace(_authService.PIN))
            {
                var lockPinPage = (currentPage?.CurrentPage as LockPinPage);
                if(lockPinPage == null)
                {
                    lockPinPage = new LockPinPage();
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(lockPinPage), false);
                    lockPinPage.PinControl.Entry.Focus();
                }
            }
            else
            {
                if((currentPage?.CurrentPage as LockPasswordPage) == null)
                {
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockPasswordPage()), false);
                }
            }
        }

        private void SetStyles()
        {
            var gray = Color.FromHex("333333");
            var grayLight = Color.FromHex("777777");
            var grayLighter = Color.FromHex("d2d6de");
            var primaryColor = Color.FromHex("3c8dbc");
            var primaryColorAccent = Color.FromHex("286090");

            Resources = new ResourceDictionary();

            // Labels

            Resources.Add("text-muted", new Style(typeof(Label))
            {
                Setters = {
                    new Setter { Property = Label.TextColorProperty, Value = grayLight }
                }
            });

            // Buttons

            Resources.Add("btn-default", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = gray }
                }
            });

            Resources.Add("btn-primary", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = Color.White },
                    new Setter { Property = Button.BackgroundColorProperty, Value = primaryColor },
                    new Setter { Property = Button.FontAttributesProperty, Value = FontAttributes.Bold },
                    new Setter { Property = Button.BorderRadiusProperty, Value = 0 }
                }
            });

            Resources.Add("btn-primaryAccent", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = primaryColorAccent }
                }
            });

            Resources.Add("btn-white", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.BackgroundColorProperty, Value = Color.White },
                    new Setter { Property = Button.TextColorProperty, Value = primaryColor },
                    new Setter { Property = Button.FontAttributesProperty, Value = FontAttributes.Bold },
                    new Setter { Property = Button.BorderRadiusProperty, Value = 0 }
                }
            });

            Resources.Add(new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = primaryColor }
                }
            });

            // List View

            Resources.Add(new Style(typeof(ListView))
            {
                Setters = {
                    new Setter { Property = ListView.SeparatorColorProperty, Value = grayLighter }
                }
            });

            // Search Bar

            Resources.Add(new Style(typeof(SearchBar))
            {
                Setters = {
                    new Setter { Property = SearchBar.CancelButtonColorProperty, Value = primaryColor }
                }
            });
        }
    }
}
