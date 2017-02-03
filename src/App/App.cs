using System;
using System.Linq;
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
using XLabs.Ioc;
using System.Reflection;
using Bit.App.Resources;
using System.Threading;

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
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;
        private readonly ILockService _lockService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly ILocalizeService _localizeService;
        private CancellationTokenSource _setMainPageCancellationTokenSource = null;

        public static bool FromAutofillService { get; set; } = false;

        public App(
            string uri,
            IAuthService authService,
            IConnectivity connectivity,
            IUserDialogs userDialogs,
            IDatabaseService databaseService,
            ISyncService syncService,
            IFingerprint fingerprint,
            ISettings settings,
            ILockService lockService,
            IGoogleAnalyticsService googleAnalyticsService,
            ILocalizeService localizeService)
        {
            _uri = uri;
            _databaseService = databaseService;
            _connectivity = connectivity;
            _userDialogs = userDialogs;
            _syncService = syncService;
            _authService = authService;
            _fingerprint = fingerprint;
            _settings = settings;
            _lockService = lockService;
            _googleAnalyticsService = googleAnalyticsService;
            _localizeService = localizeService;

            SetCulture();
            SetStyles();

            FromAutofillService = !string.IsNullOrWhiteSpace(_uri);
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
                await CheckLockAsync(args);
                await Task.Run(() => IncrementalSyncAsync()).ConfigureAwait(false);
            });

            MessagingCenter.Subscribe<Application, bool>(Current, "Lock", (sender, args) =>
            {
                Device.BeginInvokeOnMainThread(async () => await CheckLockAsync(args));
            });

            MessagingCenter.Subscribe<Application, string>(Current, "Logout", (sender, args) =>
            {
                Device.BeginInvokeOnMainThread(() => Logout(args));
            });

            MessagingCenter.Subscribe<Application>(Current, "SetMainPage", (sender) =>
            {
                _setMainPageCancellationTokenSource = SetMainPageFromAutofill(_setMainPageCancellationTokenSource, 500);
            });

            MessagingCenter.Subscribe<Application>(Current, "SetMainPageNow", (sender) =>
            {
                _setMainPageCancellationTokenSource = SetMainPageFromAutofill(_setMainPageCancellationTokenSource, 0);
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

        protected async override void OnSleep()
        {
            // Handle when your app sleeps
            Debug.WriteLine("OnSleep");
            
            if(Device.OS == TargetPlatform.Android && !TopPageIsLock())
            {
                _settings.AddOrUpdateValue(Constants.LastActivityDate, DateTime.UtcNow);
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

            if(Device.OS == TargetPlatform.Android)
            {
                await CheckLockAsync(false);
            }

            var lockPinPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockPinPage;
            if(lockPinPage != null)
            {
                lockPinPage.PinControl.Entry.FocusWithDelay();
            }
        }

        private CancellationTokenSource SetMainPageFromAutofill(CancellationTokenSource previousCts, int delay)
        {
            if(Device.OS != TargetPlatform.Android)
            {
                return null;
            }

            previousCts?.Cancel();

            var cts = new CancellationTokenSource();
            Task.Run(async () =>
            {
                await Task.Delay(delay);
                if(cts.Token.IsCancellationRequested)
                {
                    return;
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    MainPage = new MainPage();
                });

                _uri = null;
                FromAutofillService = false;
            }, cts.Token);

            return cts;
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
                        await _syncService.IncrementalSyncAsync(TimeSpan.FromMinutes(30)).ConfigureAwait(false);
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

        private async Task FullSyncAsync()
        {
            if(_connectivity.IsConnected)
            {
                var attempt = 0;
                do
                {
                    try
                    {
                        await _syncService.FullSyncAsync().ConfigureAwait(false);
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

        private async void Logout(string logoutMessage)
        {
            _authService.LogOut();

            _googleAnalyticsService.TrackAppEvent("LoggedOut");
            _googleAnalyticsService.RefreshUserId();

            Current.MainPage = new ExtendedNavigationPage(new HomePage());
            if(!string.IsNullOrWhiteSpace(logoutMessage))
            {
                _userDialogs.Toast(logoutMessage);
            }

            var deviceApiRepository = Resolver.Resolve<IDeviceApiRepository>();
            var appIdService = Resolver.Resolve<IAppIdService>();
            _settings.Remove(Constants.PushLastRegistrationDate);
            await Task.Run(() => deviceApiRepository.PutClearTokenAsync(appIdService.AppId)).ConfigureAwait(false);
        }

        private async Task CheckLockAsync(bool forceLock)
        {
            if(TopPageIsLock())
            {
                // already locked
                return;
            }

            var lockType = _lockService.GetLockType(forceLock);
            var currentPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
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
            if(Device.OS == TargetPlatform.iOS || Device.OS == TargetPlatform.Android)
            {
                var ci = _localizeService.GetCurrentCultureInfo();
                AppResources.Culture = ci;
                _localizeService.SetLocale(ci);
            }
        }
    }
}
