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
using XLabs.Ioc;

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
        private readonly ILockService _lockService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        public App(
            IAuthService authService,
            IConnectivity connectivity,
            IUserDialogs userDialogs,
            IDatabaseService databaseService,
            ISyncService syncService,
            IFingerprint fingerprint,
            ISettings settings,
            IPushNotification pushNotification,
            ILockService lockService,
            IGoogleAnalyticsService googleAnalyticsService)
        {
            _databaseService = databaseService;
            _connectivity = connectivity;
            _userDialogs = userDialogs;
            _syncService = syncService;
            _authService = authService;
            _fingerprint = fingerprint;
            _settings = settings;
            _pushNotification = pushNotification;
            _lockService = lockService;
            _googleAnalyticsService = googleAnalyticsService;

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
                Device.BeginInvokeOnMainThread(() => Logout(args));
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
                _settings.AddOrUpdateValue(Constants.LastBackgroundedDate, DateTime.UtcNow);
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
                        await _syncService.IncrementalSyncAsync(TimeSpan.FromMinutes(30));
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

        private async void Logout(string logoutMessage)
        {
            var deviceApiRepository = Resolver.Resolve<IDeviceApiRepository>();
            var appIdService = Resolver.Resolve<IAppIdService>();

            _pushNotification.Unregister();
            _settings.Remove(Constants.PushLastRegistrationDate);
            await deviceApiRepository.PutClearTokenAsync(appIdService.AppId);

            _authService.LogOut();

            _googleAnalyticsService.TrackAppEvent("LoggedOut");
            _googleAnalyticsService.RefreshUserId();

            Current.MainPage = new HomePage();
            if(!string.IsNullOrWhiteSpace(logoutMessage))
            {
                _userDialogs.Toast(logoutMessage);
            }
        }

        private async Task CheckLockAsync(bool forceLock)
        {
            var lockType = _lockService.GetLockType(forceLock);
            var currentPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
            switch(lockType)
            {
                case Enums.LockType.Fingerprint:
                    if((currentPage?.CurrentPage as LockFingerprintPage) == null)
                    {
                        await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockFingerprintPage(!forceLock)), false);
                    }
                    break;
                case Enums.LockType.PIN:
                    var lockPinPage = (currentPage?.CurrentPage as LockPinPage);
                    if(lockPinPage == null)
                    {
                        lockPinPage = new LockPinPage();
                        await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(lockPinPage), false);
                        lockPinPage.PinControl.Entry.Focus();
                    }
                    break;
                case Enums.LockType.Password:
                    if((currentPage?.CurrentPage as LockPasswordPage) == null)
                    {
                        await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockPasswordPage()), false);
                    }
                    break;
                default:
                    break;
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
