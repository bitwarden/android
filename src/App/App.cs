using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Xamarin.Forms;
using System.Diagnostics;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using Bit.App.Controls;
using Plugin.Connectivity.Abstractions;
using System.Net;
using Acr.UserDialogs;
using XLabs.Ioc;
using System.Reflection;
using Bit.App.Resources;
using Bit.App.Utilities;

namespace Bit.App
{
    public class App : Application
    {
        private string _uri;
        private readonly IDatabaseService _databaseService;
        private readonly IConnectivity _connectivity;
        private readonly IUserDialogs _userDialogs;
        private readonly ISyncService _syncService;
        private readonly IAuthService _authService;
        private readonly ISettings _settings;
        private readonly ILockService _lockService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly ILocalizeService _localizeService;
        private readonly IAppInfoService _appInfoService;
        private readonly IAppSettingsService _appSettingsService;
        private readonly IDeviceActionService _deviceActionService;

        public App(
            string uri,
            IAuthService authService,
            IConnectivity connectivity,
            IUserDialogs userDialogs,
            IDatabaseService databaseService,
            ISyncService syncService,
            ISettings settings,
            ILockService lockService,
            IGoogleAnalyticsService googleAnalyticsService,
            ILocalizeService localizeService,
            IAppInfoService appInfoService,
            IAppSettingsService appSettingsService,
            IDeviceActionService deviceActionService)
        {
            _uri = uri;
            _databaseService = databaseService;
            _connectivity = connectivity;
            _userDialogs = userDialogs;
            _syncService = syncService;
            _authService = authService;
            _settings = settings;
            _lockService = lockService;
            _googleAnalyticsService = googleAnalyticsService;
            _localizeService = localizeService;
            _appInfoService = appInfoService;
            _appSettingsService = appSettingsService;
            _deviceActionService = deviceActionService;

            SetCulture();
            SetStyles();

            if(authService.IsAuthenticated && _uri != null)
            {
                MainPage = new ExtendedNavigationPage(new VaultAutofillListLoginsPage(_uri));
            }
            else if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new ExtendedNavigationPage(new HomePage());
            }

            MessagingCenter.Subscribe<Application, bool>(Current, "Resumed", async (sender, args) =>
            {
                Device.BeginInvokeOnMainThread(async () => await CheckLockAsync(args));
                await Task.Run(() => FullSyncAsync()).ConfigureAwait(false);
            });

            MessagingCenter.Subscribe<Application, bool>(Current, "Lock", (sender, args) =>
            {
                Device.BeginInvokeOnMainThread(async () => await CheckLockAsync(args));
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

            if(string.IsNullOrWhiteSpace(_uri))
            {
                Helpers.PerformUpdateTasks(_settings, _appInfoService, _databaseService);
                await Task.Run(() => FullSyncAsync()).ConfigureAwait(false);
            }

            if((DateTime.UtcNow - _appSettingsService.LastCacheClear).TotalDays >= 1)
            {
                await Task.Run(() => _deviceActionService.ClearCache()).ConfigureAwait(false);
            }

            Debug.WriteLine("OnStart");
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
            Debug.WriteLine("OnSleep");

            SetMainPageFromAutofill();

            if(Device.RuntimePlatform == Device.Android && !TopPageIsLock())
            {
                _lockService.UpdateLastActivity();
            }
        }

        protected async override void OnResume()
        {
            base.OnResume();

            // workaround for app compat bug
            // ref https://forums.xamarin.com/discussion/62414/app-resuming-results-in-crash-with-formsappcompatactivity
            await Task.Delay(10);

            // Handle when your app resumes
            Debug.WriteLine("OnResume");

            if(Device.RuntimePlatform == Device.Android)
            {
                await CheckLockAsync(false);
            }

            var lockPinPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockPinPage;
            if(lockPinPage != null)
            {
                lockPinPage.PinControl.Entry.FocusWithDelay();
            }

            if(Device.RuntimePlatform == Device.Android)
            {
                await Task.Run(() => FullSyncAsync()).ConfigureAwait(false);
            }

            var now = DateTime.UtcNow;
            if((now - _appSettingsService.LastCacheClear).TotalDays >= 1
                && (now - _appSettingsService.LastActivity).TotalHours >= 1)
            {
                await Task.Run(() => _deviceActionService.ClearCache()).ConfigureAwait(false);
            }
        }

        private void SetMainPageFromAutofill()
        {
            if(Device.RuntimePlatform == Device.Android && !string.IsNullOrWhiteSpace(_uri))
            {
                Task.Run(() =>
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        Current.MainPage = new MainPage();
                        _uri = null;
                    });
                });
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
                        await _syncService.FullSyncAsync(TimeSpan.FromMinutes(30)).ConfigureAwait(false);
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
                    catch(Exception e) when(e is TaskCanceledException || e is OperationCanceledException)
                    {
                        Debug.WriteLine("Cancellation exception.");
                        break;
                    }
                } while(attempt <= 1);
            }
            else
            {
                Debug.WriteLine("Not connected.");
            }
        }

        private void Logout(string logoutMessage)
        {
            _authService.LogOut();

            var deviceApiRepository = Resolver.Resolve<IDeviceApiRepository>();
            var appIdService = Resolver.Resolve<IAppIdService>();
            Task.Run(async () => await deviceApiRepository.PutClearTokenAsync(appIdService.AppId));

            _googleAnalyticsService.TrackAppEvent("LoggedOut");

            Device.BeginInvokeOnMainThread(() => Current.MainPage = new ExtendedNavigationPage(new HomePage()));
            if(!string.IsNullOrWhiteSpace(logoutMessage))
            {
                _userDialogs.Toast(logoutMessage);
            }
        }

        private async Task CheckLockAsync(bool forceLock)
        {
            if(TopPageIsLock())
            {
                // already locked
                return;
            }

            var lockType = await _lockService.GetLockTypeAsync(forceLock);
            if(lockType == Enums.LockType.None)
            {
                return;
            }

            _appSettingsService.Locked = true;
            switch(lockType)
            {
                case Enums.LockType.Fingerprint:
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockFingerprintPage(!forceLock)), false);
                    break;
                case Enums.LockType.PIN:
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockPinPage()), false);
                    break;
                case Enums.LockType.Password:
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockPasswordPage()), false);
                    break;
                default:
                    break;
            }
        }

        private bool TopPageIsLock()
        {
            var currentPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
            if((currentPage?.CurrentPage as LockFingerprintPage) != null)
            {
                return true;
            }
            if((currentPage?.CurrentPage as LockPinPage) != null)
            {
                return true;
            }
            if((currentPage?.CurrentPage as LockPasswordPage) != null)
            {
                return true;
            }

            return false;
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

            Resources.Add(new Style(typeof(ExtendedButton))
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

        private void SetCulture()
        {
            Debug.WriteLine("====== resource debug info =========");
            var assembly = typeof(App).GetTypeInfo().Assembly;
            foreach(var res in assembly.GetManifestResourceNames())
            {
                Debug.WriteLine("found resource: " + res);
            }
            Debug.WriteLine("====================================");

            // This lookup NOT required for Windows platforms - the Culture will be automatically set
            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Android)
            {
                var ci = _localizeService.GetCurrentCultureInfo();
                AppResources.Culture = ci;
                _localizeService.SetLocale(ci);
            }
        }
    }
}
