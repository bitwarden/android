using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.Core.Resources.Localization;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.App.Utilities.AccountManagement;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Response;
using Bit.Core.Pages;
using Bit.Core.Services;
using Bit.Core.Utilities;

[assembly: XamlCompilation(XamlCompilationOptions.Compile)]
namespace Bit.App
{
    public partial class App : Application, IAccountsManagerHost
    {
        public const string POP_ALL_AND_GO_TO_TAB_GENERATOR_MESSAGE = "popAllAndGoToTabGenerator";
        public const string POP_ALL_AND_GO_TO_TAB_MYVAULT_MESSAGE = "popAllAndGoToTabMyVault";
        public const string POP_ALL_AND_GO_TO_TAB_SEND_MESSAGE = "popAllAndGoToTabSend";
        public const string POP_ALL_AND_GO_TO_AUTOFILL_CIPHERS_MESSAGE = "popAllAndGoToAutofillCiphers";

        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ISyncService _syncService;
        private readonly IAuthService _authService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IFileService _fileService;
        private readonly IAccountsManager _accountsManager;
        private readonly IPushNotificationService _pushNotificationService;
        private readonly IConfigService _configService;
        private static bool _isResumed;
        // these variables are static because the app is launching new activities on notification click, creating new instances of App. 
        private static bool _pendingCheckPasswordlessLoginRequests;
        private static object _processingLoginRequestLock = new object();

        // [MAUI-Migration] Workaround to avoid issue on Android where trying to show the LockPage when the app is resuming or in background breaks the app.
        // This queue keeps those actions so that when the app has resumed they can still be executed.
        // Links: https://github.com/dotnet/maui/issues/11501 and https://bitwarden.atlassian.net/wiki/spaces/NMME/pages/664862722/MainPage+Assignments+not+working+on+Android+on+Background+or+App+resume
        private readonly Queue<Action> _onResumeActions = new Queue<Action>();

#if ANDROID

        /*
         *  ** Workaround for our Android crashes when trying to use Autofill **
         *
         * This workaround works by managing the "Window Creation" ourselves. When we get an Autofill initialization we should create a new window instead of reusing the "Main/Current Window".
         * While this workaround works, it's hard to execute the "workflow" that devices where we should navigate to. Below are some of the things we tried:
         *  1 - Tried creating "new Window(new NavigationPage)" and invoking the code for handling the Navigations afterward. Issue with this is that the code that handles the navigations doesn't know which "Window" to use and calls the default "Window.Page"
         *  2 - Tried using CustomWindow implementations to track the "WindowCreated" event and to be able to distinguish the different Window types (Default Window or Window for Autofill for example).
         *      This solution had a bit of overhear work and still required the app to set something line "new Window(new NavigationPage)" before actually knowing where we wanted to navigate to.
         *
         * Ideally we could figure out the Navigation we want to do before CreateWindow (on MainActivity.OnCreate) for example. But this needs to be done in async anyway we can't do async in both CreateWindow and MainActivity.OnCreate
         */

        public new static Page MainPage
        {
            get
            {
                return CurrentWindow?.Page;
            }
            set
            {
                if (CurrentWindow != null)
                {
                    CurrentWindow.Page = value;
                }
            }
        }   

        public static Window CurrentWindow { get; private set; }

        private Window _autofillWindow;
        private Window _mainWindow;

        public void SetOptions(AppOptions appOptions)
        {
            Options = appOptions ?? new AppOptions();
        }
        
        protected override Window CreateWindow(IActivationState activationState)
        {
            if (activationState != null 
                && activationState.State.TryGetValue("autofillFramework", out string autofillFramework)
                && autofillFramework == "true"
                && activationState.State.ContainsKey("autofillFrameworkCipherId")) //AutofillExternalActivity
            {
                return new Window(new NavigationPage()); //No actual page needed. Only used for auto-filling the fields directly (externally)
            }

            if (Options != null && (Options.FromAutofillFramework || Options.Uri != null || Options.OtpData != null || Options.CreateSend != null)) //"Internal" Autofill and Uri/Otp/CreateSend
            {
                _autofillWindow = new Window(new NavigationPage(new AndroidExtSplashPage(Options)));
                CurrentWindow = _autofillWindow;
                return CurrentWindow;
            }

            if(CurrentWindow != null)
            {
                //TODO: This likely crashes if we try to have two apps side-by-side on Android
                //TODO: Question: In these scenarios should a new Window be created or can the same one be reused?
                CurrentWindow = _mainWindow;
                return CurrentWindow;
            }

            _mainWindow = new Window(new NavigationPage(new HomePage(Options)));
            CurrentWindow = _mainWindow;
            return CurrentWindow;
        }
#elif IOS
        public new static Page MainPage
        {
            get
            {
                return Application.Current.MainPage;
            }
            set
            {
                Application.Current.MainPage = value;
            }
        }   
#endif

        public App() : this(null)
        {
        }

        public App(AppOptions appOptions)
        {
            Options = appOptions ?? new AppOptions();
            if (Options.IosExtension)
            {
                Current = this;
                return;
            }
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _fileService = ServiceContainer.Resolve<IFileService>();
            _accountsManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");
            _pushNotificationService = ServiceContainer.Resolve<IPushNotificationService>();
            _configService = ServiceContainer.Resolve<IConfigService>();

            _accountsManager.Init(() => Options, this);

            Bootstrap();
            _broadcasterService.Subscribe(nameof(App), async (message) =>
            {
                try
                {
                    if (message.Command == "showDialog")
                    {
                        var details = message.Data as DialogDetails;
                        var confirmed = true;
                        var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ?
                            AppResources.Ok : details.ConfirmText;
                        MainThread.BeginInvokeOnMainThread(async () =>
                        {
                            if (!string.IsNullOrWhiteSpace(details.CancelText))
                            {
                                confirmed = await MainPage.DisplayAlert(details.Title, details.Text, confirmText,
                                    details.CancelText);
                            }
                            else
                            {
                                await MainPage.DisplayAlert(details.Title, details.Text, confirmText);
                            }
                            _messagingService.Send("showDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
                        });
                    }
                    else if (message.Command == AppHelpers.RESUMED_MESSAGE_COMMAND)
                    {
                        if (DeviceInfo.Platform == DevicePlatform.iOS)
                        {
                            ResumedAsync().FireAndForget();
                        }
                    }
                    else if (message.Command == "slept")
                    {
                        if (DeviceInfo.Platform == DevicePlatform.iOS)
                        {
                            await SleptAsync();
                        }
                    }
                    else if (message.Command == "migrated")
                    {
                        await Task.Delay(1000);
                        await _accountsManager.NavigateOnAccountChangeAsync();
                    }
                    else if (message.Command == POP_ALL_AND_GO_TO_TAB_GENERATOR_MESSAGE ||
                        message.Command == POP_ALL_AND_GO_TO_TAB_MYVAULT_MESSAGE ||
                        message.Command == POP_ALL_AND_GO_TO_TAB_SEND_MESSAGE ||
                        message.Command == POP_ALL_AND_GO_TO_AUTOFILL_CIPHERS_MESSAGE ||
                        message.Command == DeepLinkContext.NEW_OTP_MESSAGE)
                    {
                        if (message.Command == DeepLinkContext.NEW_OTP_MESSAGE)
                        {
                            Options.OtpData = new OtpData((string)message.Data);
                        }

                        MainThread.InvokeOnMainThreadAsync(async () =>
                        {
                            if (MainPage is TabsPage tabsPage)
                            {
                                while (tabsPage.Navigation.ModalStack.Count > 0)
                                {
                                    await tabsPage.Navigation.PopModalAsync(false);
                                }
                                if (message.Command == POP_ALL_AND_GO_TO_AUTOFILL_CIPHERS_MESSAGE)
                                {
                                    MainPage = new NavigationPage(new CipherSelectionPage(Options));
                                }
                                else if (message.Command == POP_ALL_AND_GO_TO_TAB_MYVAULT_MESSAGE)
                                {
                                    Options.MyVaultTile = false;
                                    tabsPage.ResetToVaultPage();
                                }
                                else if (message.Command == POP_ALL_AND_GO_TO_TAB_GENERATOR_MESSAGE)
                                {
                                    Options.GeneratorTile = false;
                                    tabsPage.ResetToGeneratorPage();
                                }
                                else if (message.Command == POP_ALL_AND_GO_TO_TAB_SEND_MESSAGE)
                                {
                                    tabsPage.ResetToSendPage();
                                }
                                else if (message.Command == DeepLinkContext.NEW_OTP_MESSAGE)
                                {
                                    tabsPage.ResetToVaultPage();
                                    await tabsPage.Navigation.PushModalAsync(new NavigationPage(new CipherSelectionPage(Options)));
                                }
                            }
                        });
                    }
                    else if (message.Command == "convertAccountToKeyConnector")
                    {
                        MainThread.BeginInvokeOnMainThread(async () =>
                        {
                            await MainPage.Navigation.PushModalAsync(
                                new NavigationPage(new RemoveMasterPasswordPage()));
                        });
                    }
                    else if (message.Command == Constants.ForceUpdatePassword)
                    {
                        MainThread.BeginInvokeOnMainThread(async () =>
                        {
                            await MainPage.Navigation.PushModalAsync(
                                new NavigationPage(new UpdateTempPasswordPage()));
                        });
                    }
                    else if (message.Command == Constants.ForceSetPassword)
                    {
                        await MainThread.InvokeOnMainThreadAsync(() => MainPage.Navigation.PushModalAsync(
                                new NavigationPage(new SetPasswordPage(orgIdentifier: (string)message.Data))));
                    }
                    else if (message.Command == "syncCompleted")
                    {
                        await _configService.GetAsync(true);
                    }
                    else if (message.Command == Constants.PasswordlessLoginRequestKey
                        || message.Command == "unlocked"
                        || message.Command == AccountsManagerMessageCommands.ACCOUNT_SWITCH_COMPLETED)
                    {
                        lock (_processingLoginRequestLock)
                        {
                            // lock doesn't allow for async execution
                            CheckPasswordlessLoginRequestsAsync().Wait();
                        }
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
        }

        private async Task CheckPasswordlessLoginRequestsAsync()
        {
            if (!_isResumed)
            {
                _pendingCheckPasswordlessLoginRequests = true;
                return;
            }
            _pendingCheckPasswordlessLoginRequests = false;
            if (await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }

            var notification = await _stateService.GetPasswordlessLoginNotificationAsync();
            if (notification == null)
            {
                return;
            }

            if (await CheckShouldSwitchActiveUserAsync(notification))
            {
                return;
            }

            // Delay to wait for the vault page to appear
            await Task.Delay(2000);
            // if there is a request modal opened ignore all incoming requests
            if (MainPage.Navigation.ModalStack.Any(p => p is NavigationPage navPage && navPage.CurrentPage is LoginPasswordlessPage))
            {
                return;
            }
            var loginRequestData = await _authService.GetPasswordlessLoginRequestByIdAsync(notification.Id);
            var page = new LoginPasswordlessPage(new LoginPasswordlessDetails()
            {
                PubKey = loginRequestData.PublicKey,
                Id = loginRequestData.Id,
                IpAddress = loginRequestData.RequestIpAddress,
                Email = await _stateService.GetEmailAsync(),
                FingerprintPhrase = loginRequestData.FingerprintPhrase,
                RequestDate = loginRequestData.CreationDate,
                DeviceType = loginRequestData.RequestDeviceType,
                Origin = loginRequestData.Origin
            });
            await _stateService.SetPasswordlessLoginNotificationAsync(null);
            _pushNotificationService.DismissLocalNotification(Constants.PasswordlessNotificationId);
            if (!loginRequestData.IsExpired)
            {
                await MainThread.InvokeOnMainThreadAsync(() => MainPage.Navigation.PushModalAsync(new NavigationPage(page)));
            }
        }

        private async Task<bool> CheckShouldSwitchActiveUserAsync(PasswordlessRequestNotification notification)
        {
            var activeUserId = await _stateService.GetActiveUserIdAsync();
            if (notification.UserId == activeUserId)
            {
                return false;
            }

            var notificationUserEmail = await _stateService.GetEmailAsync(notification.UserId);
            MainThread.BeginInvokeOnMainThread(async () =>
            {
                try
                {
                    var result = await _deviceActionService.DisplayAlertAsync(AppResources.LogInRequested, string.Format(AppResources.LoginAttemptFromXDoYouWantToSwitchToThisAccount, notificationUserEmail), AppResources.Cancel, AppResources.Ok);
                    if (result == AppResources.Ok)
                    {
                        await _stateService.SetActiveUserAsync(notification.UserId);
                        _messagingService.Send(AccountsManagerMessageCommands.SWITCHED_ACCOUNT);
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
            return true;
        }

        public AppOptions Options { get; private set; }

        protected override async void OnStart()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnStart");
            _isResumed = true;
            await ClearCacheIfNeededAsync();
            Prime();
            if (string.IsNullOrWhiteSpace(Options.Uri))
            {
                var updated = await AppHelpers.PerformUpdateTasksAsync(_syncService, _deviceActionService,
                    _stateService);
                if (!updated)
                {
                    SyncIfNeeded();
                }
            }
            if (_pendingCheckPasswordlessLoginRequests)
            {
                _messagingService.Send(Constants.PasswordlessLoginRequestKey);
            }
            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                await _vaultTimeoutService.CheckVaultTimeoutAsync();
                // Reset delay on every start
                _vaultTimeoutService.DelayLockAndLogoutMs = null;
            }

            await _configService.GetAsync();
            _messagingService.Send("startEventTimer");
        }

        protected override async void OnSleep()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnSleep");
            _isResumed = false;
            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                var isLocked = await _vaultTimeoutService.IsLockedAsync();
                if (!isLocked)
                {
                    await _stateService.SetLastActiveTimeAsync(_deviceActionService.GetActiveTime());
                }
                if (!SetTabsPageFromAutofill(isLocked))
                {
                    ClearAutofillUri();
                }
                await SleptAsync();
            }
        }

        protected override void OnResume()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnResume");
            _isResumed = true;
            if (_pendingCheckPasswordlessLoginRequests)
            {
                _messagingService.Send(Constants.PasswordlessLoginRequestKey);
            }
            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                ResumedAsync().FireAndForget();
            }
        }

        private async Task SleptAsync()
        {
            await _vaultTimeoutService.CheckVaultTimeoutAsync();
            await ClearSensitiveFieldsAsync();
            _messagingService.Send("stopEventTimer");
        }

        private async Task ResumedAsync()
        {
            await _stateService.CheckExtensionActiveUserAndSwitchIfNeededAsync();
            await _vaultTimeoutService.CheckVaultTimeoutAsync();
            await ClearSensitiveFieldsAsync();
            _messagingService.Send("startEventTimer");
            await UpdateThemeAsync();
            await ClearCacheIfNeededAsync();
            Prime();
            SyncIfNeeded();
            if (MainPage is NavigationPage navPage && navPage.CurrentPage is LockPage lockPage)
            {
                await lockPage.PromptBiometricAfterResumeAsync();
            }

            // [MAUI-Migration] Workaround to avoid issue on Android where trying to show the LockPage when the app is resuming or in background breaks the app.
            // Currently we keep those actions in a queue until the app has resumed and execute them here.
            // Links: https://github.com/dotnet/maui/issues/11501 and https://bitwarden.atlassian.net/wiki/spaces/NMME/pages/664862722/MainPage+Assignments+not+working+on+Android+on+Background+or+App+resume
            await Task.Delay(50); //Small delay that is part of the workaround and ensures the app is ready to set "MainPage"
            while (_onResumeActions.TryDequeue(out var action))
            {
                MainThread.BeginInvokeOnMainThread(action);
            }
        }

        public async Task UpdateThemeAsync()
        {
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                ThemeManager.SetTheme(Resources);
                _messagingService.Send(ThemeManager.UPDATED_THEME_MESSAGE_KEY);
            });
        }

        private async Task ClearSensitiveFieldsAsync()
        {
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                _messagingService.Send(Constants.ClearSensitiveFields);
            });
        }

        private void SetCulture()
        {
            // Calendars are removed by linker. ref https://bugzilla.xamarin.com/show_bug.cgi?id=59077
            new System.Globalization.ThaiBuddhistCalendar();
            new System.Globalization.HijriCalendar();
            new System.Globalization.UmAlQuraCalendar();
        }

        private async Task ClearCacheIfNeededAsync()
        {
            var lastClear = await _stateService.GetLastFileCacheClearAsync();
            if ((DateTime.UtcNow - lastClear.GetValueOrDefault(DateTime.MinValue)).TotalDays >= 1)
            {
                var task = Task.Run(() => _fileService.ClearCacheAsync());
            }
        }

        private void ClearAutofillUri()
        {
            if (DeviceInfo.Platform == DevicePlatform.Android && !string.IsNullOrWhiteSpace(Options.Uri))
            {
                Options.Uri = null;
            }
        }

        private bool SetTabsPageFromAutofill(bool isLocked)
        {
            if (DeviceInfo.Platform == DevicePlatform.Android && !string.IsNullOrWhiteSpace(Options.Uri) &&
                !Options.FromAutofillFramework)
            {
                Task.Run(() =>
                {
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        Options.Uri = null;
                        if (isLocked)
                        {
                            App.MainPage = new NavigationPage(new LockPage());
                        }
                        else
                        {
                            App.MainPage = new TabsPage();
                        }
                    });
                });
                return true;
            }
            return false;
        }

        private void Prime()
        {
            Task.Run(() =>
            {
                var word = EEFLongWordList.Instance.List[1];
                var parsedDomain = DomainName.TryParse("https://bitwarden.com", out var domainName);
            });
        }

        private void Bootstrap()
        {
            InitializeComponent();
            SetCulture();
            ThemeManager.SetTheme(Resources);
            RequestedThemeChanged += (s, a) =>
            {
                UpdateThemeAsync();
            };
            _accountsManager.NavigateOnAccountChangeAsync().FireAndForget();
            ServiceContainer.Resolve<MobilePlatformUtilsService>("platformUtilsService").Init();
        }

        private void SyncIfNeeded()
        {
            if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
            {
                return;
            }
            Task.Run(async () =>
            {
                var lastSync = await _syncService.GetLastSyncAsync();
                if (lastSync == null || ((DateTime.UtcNow - lastSync) > TimeSpan.FromMinutes(30)))
                {
                    await Task.Delay(1000);
                    await _syncService.FullSyncAsync(false);
                }
            });
        }

        public async Task SetPreviousPageInfoAsync()
        {
            PreviousPageInfo lastPageBeforeLock = null;
            if (MainPage is TabbedPage tabbedPage && tabbedPage.Navigation.ModalStack.Count > 0)
            {
                var topPage = tabbedPage.Navigation.ModalStack[tabbedPage.Navigation.ModalStack.Count - 1];
                if (topPage is NavigationPage navPage)
                {
                    if (navPage.CurrentPage is CipherDetailsPage cipherDetailsPage)
                    {
                        lastPageBeforeLock = new PreviousPageInfo
                        {
                            Page = "view",
                            CipherId = cipherDetailsPage.ViewModel.CipherId
                        };
                    }
                    else if (navPage.CurrentPage is CipherAddEditPage cipherAddEditPage && cipherAddEditPage.ViewModel.EditMode)
                    {
                        lastPageBeforeLock = new PreviousPageInfo
                        {
                            Page = "edit",
                            CipherId = cipherAddEditPage.ViewModel.CipherId
                        };
                    }
                }
            }
            await _stateService.SetPreviousPageInfoAsync(lastPageBeforeLock);
        }

        public void Navigate(NavigationTarget navTarget, INavigationParams navParams)
        {

            // [MAUI-Migration] Workaround to avoid issue on Android where trying to show the LockPage when the app is resuming or in background breaks the app.
            // If we are in background we add the Navigation Actions to a queue to execute when the app resumes.
            // Links: https://github.com/dotnet/maui/issues/11501 and https://bitwarden.atlassian.net/wiki/spaces/NMME/pages/664862722/MainPage+Assignments+not+working+on+Android+on+Background+or+App+resume
#if ANDROID
            if (!_isResumed)
            {
                _onResumeActions.Enqueue(() => NavigateImpl(navTarget, navParams));
                return;
            }
#endif
            NavigateImpl(navTarget, navParams);
        }

        private void NavigateImpl(NavigationTarget navTarget, INavigationParams navParams)
        {
            switch (navTarget)
            {
                case NavigationTarget.HomeLogin:
                    App.MainPage = new NavigationPage(new HomePage(Options));
                    break;
                case NavigationTarget.Login:
                    if (navParams is LoginNavigationParams loginParams)
                    {
                        App.MainPage = new NavigationPage(new LoginPage(loginParams.Email, Options));
                    }
                    break;
                case NavigationTarget.Lock:
                    if (navParams is LockNavigationParams lockParams)
                    {
                        App.MainPage = new NavigationPage(new LockPage(Options, lockParams.AutoPromptBiometric));
                    }
                    else
                    {
                        App.MainPage = new NavigationPage(new LockPage(Options));
                    }
                    break;
                case NavigationTarget.Home:
                    App.MainPage = new TabsPage(Options);
                    break;
                case NavigationTarget.AddEditCipher:
                    App.MainPage = new NavigationPage(new CipherAddEditPage(appOptions: Options));
                    break;
                case NavigationTarget.AutofillCiphers:
                case NavigationTarget.OtpCipherSelection:
                    App.MainPage = new NavigationPage(new CipherSelectionPage(Options));
                    break;
                case NavigationTarget.SendAddEdit:
                    App.MainPage = new NavigationPage(new SendAddEditPage(Options));
                    break;
            }
        }
    }
}
