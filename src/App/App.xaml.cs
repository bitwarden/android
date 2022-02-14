using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

[assembly: XamlCompilation(XamlCompilationOptions.Compile)]
namespace Bit.App
{
    public partial class App : Application
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ISyncService _syncService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IAuthService _authService;
        private readonly IStorageService _secureStorageService;
        private readonly IDeviceActionService _deviceActionService;

        private static bool _isResumed;

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
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");

            Bootstrap();
            _broadcasterService.Subscribe(nameof(App), async (message) =>
            {
                if (message.Command == "showDialog")
                {
                    var details = message.Data as DialogDetails;
                    var confirmed = true;
                    var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ?
                        AppResources.Ok : details.ConfirmText;
                    Device.BeginInvokeOnMainThread(async () =>
                    {
                        if (!string.IsNullOrWhiteSpace(details.CancelText))
                        {
                            confirmed = await Current.MainPage.DisplayAlert(details.Title, details.Text, confirmText,
                                details.CancelText);
                        }
                        else
                        {
                            await Current.MainPage.DisplayAlert(details.Title, details.Text, confirmText);
                        }
                        _messagingService.Send("showDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
                    });
                }
                else if (message.Command == "locked")
                {
                    await LockedAsync(!(message.Data as bool?).GetValueOrDefault());
                }
                else if (message.Command == "lockVault")
                {
                    await _vaultTimeoutService.LockAsync(true);
                }
                else if (message.Command == "logout")
                {
                    var extras = message.Data as Tuple<string, bool, bool>;
                    var userId = extras?.Item1;
                    var userInitiated = extras?.Item2;
                    var expired = extras?.Item3;
                    Device.BeginInvokeOnMainThread(async () =>
                        await LogOutAsync(userId, userInitiated, expired));
                }
                else if (message.Command == "loggedOut")
                {
                    // Clean up old migrated key if they ever log out.
                    await _secureStorageService.RemoveAsync("oldKey");
                }
                else if (message.Command == "resumed")
                {
                    if (Device.RuntimePlatform == Device.iOS)
                    {
                        ResumedAsync().FireAndForget();
                    }
                }
                else if (message.Command == "slept")
                {
                    if (Device.RuntimePlatform == Device.iOS)
                    {
                        await SleptAsync();
                    }
                }
                else if (message.Command == "addAccount")
                {
                    await AddAccount();
                }
                else if (message.Command == "accountAdded")
                {
                    await UpdateThemeAsync();
                }
                else if (message.Command == "switchedAccount")
                {
                    await SwitchedAccountAsync();
                }
                else if (message.Command == "migrated")
                {
                    await Task.Delay(1000);
                    await SetMainPageAsync();
                }
                else if (message.Command == "popAllAndGoToTabGenerator" ||
                    message.Command == "popAllAndGoToTabMyVault" ||
                    message.Command == "popAllAndGoToTabSend" ||
                    message.Command == "popAllAndGoToAutofillCiphers")
                {
                    Device.BeginInvokeOnMainThread(async () =>
                    {
                        if (Current.MainPage is TabsPage tabsPage)
                        {
                            while (tabsPage.Navigation.ModalStack.Count > 0)
                            {
                                await tabsPage.Navigation.PopModalAsync(false);
                            }
                            if (message.Command == "popAllAndGoToAutofillCiphers")
                            {
                                Current.MainPage = new NavigationPage(new AutofillCiphersPage(Options));
                            }
                            else if (message.Command == "popAllAndGoToTabMyVault")
                            {
                                Options.MyVaultTile = false;
                                tabsPage.ResetToVaultPage();
                            }
                            else if (message.Command == "popAllAndGoToTabGenerator")
                            {
                                Options.GeneratorTile = false;
                                tabsPage.ResetToGeneratorPage();
                            }
                            else if (message.Command == "popAllAndGoToTabSend")
                            {
                                tabsPage.ResetToSendPage();
                            }
                        }
                    });
                }
                else if (message.Command == "convertAccountToKeyConnector")
                {
                    Device.BeginInvokeOnMainThread(async () =>
                    {
                        await Application.Current.MainPage.Navigation.PushModalAsync(
                            new NavigationPage(new RemoveMasterPasswordPage()));
                    });
                }

            });
        }

        public AppOptions Options { get; private set; }

        protected async override void OnStart()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnStart");
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
            if (Device.RuntimePlatform == Device.Android)
            {
                await _vaultTimeoutService.CheckVaultTimeoutAsync();
                // Reset delay on every start
                _vaultTimeoutService.DelayLockAndLogoutMs = null;
            }
            _messagingService.Send("startEventTimer");
        }

        protected async override void OnSleep()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnSleep");
            _isResumed = false;
            if (Device.RuntimePlatform == Device.Android)
            {
                var isLocked = await _vaultTimeoutService.IsLockedAsync();
                if (!isLocked)
                {
                    await _stateService.SetLastActiveTimeAsync(_deviceActionService.GetActiveTime());
                }
                SetTabsPageFromAutofill(isLocked);
                await SleptAsync();
            }
        }

        protected override void OnResume()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnResume");
            _isResumed = true;
            if (Device.RuntimePlatform == Device.Android)
            {
                ResumedAsync().FireAndForget();
            }
        }

        private async Task SleptAsync()
        {
            await _vaultTimeoutService.CheckVaultTimeoutAsync();
            _messagingService.Send("stopEventTimer");
        }

        private async Task ResumedAsync()
        {
            await _vaultTimeoutService.CheckVaultTimeoutAsync();
            _messagingService.Send("startEventTimer");
            await UpdateThemeAsync();
            await ClearCacheIfNeededAsync();
            Prime();
            SyncIfNeeded();
            if (Current.MainPage is NavigationPage navPage && navPage.CurrentPage is LockPage lockPage)
            {
                await lockPage.PromptBiometricAfterResumeAsync();
            }
        }

        public async Task UpdateThemeAsync()
        {
            await Device.InvokeOnMainThreadAsync(() =>
            {
                ThemeManager.SetTheme(Current.Resources);
                _messagingService.Send("updatedTheme");
            });
        }

        private void SetCulture()
        {
            // Calendars are removed by linker. ref https://bugzilla.xamarin.com/show_bug.cgi?id=59077
            new System.Globalization.ThaiBuddhistCalendar();
            new System.Globalization.HijriCalendar();
            new System.Globalization.UmAlQuraCalendar();
        }

        private async Task LogOutAsync(string userId, bool? userInitiated, bool? expired)
        {
            await AppHelpers.LogOutAsync(userId, userInitiated.GetValueOrDefault(true));
            await SetMainPageAsync();
            _authService.LogOut(() =>
            {
                if (expired.GetValueOrDefault())
                {
                    _platformUtilsService.ShowToast("warning", null, AppResources.LoginExpired);
                }
            });
        }

        private async Task AddAccount()
        {
            Device.BeginInvokeOnMainThread(async () =>
            {
                Current.MainPage = new NavigationPage(new HomePage(Options));
            });
        }

        private async Task SwitchedAccountAsync()
        {
            await AppHelpers.OnAccountSwitchAsync();
            var shouldTimeout = await _vaultTimeoutService.ShouldTimeoutAsync();
            Device.BeginInvokeOnMainThread(async () =>
            {
                if (shouldTimeout)
                {
                    await _vaultTimeoutService.ExecuteTimeoutActionAsync();
                }
                else
                {
                    await SetMainPageAsync();
                }
                await Task.Delay(50);
                await UpdateThemeAsync();
            });
        }

        private async Task SetMainPageAsync()
        {
            var authed = await _stateService.IsAuthenticatedAsync();
            if (authed)
            {
                var isLocked = await _vaultTimeoutService.IsLockedAsync();
                var shouldTimeout = await _vaultTimeoutService.ShouldTimeoutAsync();
                var vaultTimeoutAction = await _stateService.GetVaultTimeoutActionAsync();
                if (isLocked || shouldTimeout)
                {
                    if (vaultTimeoutAction == "logOut")
                    {
                        var email = await _stateService.GetEmailAsync();
                        Current.MainPage = new NavigationPage(new LoginPage(email, Options));
                    }
                    else
                    {
                        Current.MainPage = new NavigationPage(new LockPage(Options));
                    }
                }
                else if (Options.FromAutofillFramework && Options.SaveType.HasValue)
                {
                    Current.MainPage = new NavigationPage(new AddEditPage(appOptions: Options));
                }
                else if (Options.Uri != null)
                {
                    Current.MainPage = new NavigationPage(new AutofillCiphersPage(Options));
                }
                else if (Options.CreateSend != null)
                {
                    Current.MainPage = new NavigationPage(new SendAddEditPage(Options));
                }
                else
                {
                    Current.MainPage = new TabsPage(Options);
                }
            }
            else
            {
                Current.MainPage = new NavigationPage(new HomePage(Options));
            }
        }

        private async Task ClearCacheIfNeededAsync()
        {
            var lastClear = await _stateService.GetLastFileCacheClearAsync();
            if ((DateTime.UtcNow - lastClear.GetValueOrDefault(DateTime.MinValue)).TotalDays >= 1)
            {
                var task = Task.Run(() => _deviceActionService.ClearCacheAsync());
            }
        }

        private void SetTabsPageFromAutofill(bool isLocked)
        {
            if (Device.RuntimePlatform == Device.Android && !string.IsNullOrWhiteSpace(Options.Uri) &&
                !Options.FromAutofillFramework)
            {
                Task.Run(() =>
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        Options.Uri = null;
                        if (isLocked)
                        {
                            Current.MainPage = new NavigationPage(new LockPage());
                        }
                        else
                        {
                            Current.MainPage = new TabsPage();
                        }
                    });
                });
            }
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
            ThemeManager.SetTheme(Current.Resources);
            Current.RequestedThemeChanged += (s, a) =>
            {
                UpdateThemeAsync();
            };
            Current.MainPage = new NavigationPage(new HomePage(Options));
            var mainPageTask = SetMainPageAsync();
            ServiceContainer.Resolve<MobilePlatformUtilsService>("platformUtilsService").Init();
        }

        private void SyncIfNeeded()
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
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

        private async Task LockedAsync(bool autoPromptBiometric)
        {
            if (autoPromptBiometric && Device.RuntimePlatform == Device.iOS)
            {
                var vaultTimeout = await _stateService.GetVaultTimeoutAsync();
                if (vaultTimeout == 0)
                {
                    autoPromptBiometric = false;
                }
            }
            PreviousPageInfo lastPageBeforeLock = null;
            if (Current.MainPage is TabbedPage tabbedPage && tabbedPage.Navigation.ModalStack.Count > 0)
            {
                var topPage = tabbedPage.Navigation.ModalStack[tabbedPage.Navigation.ModalStack.Count - 1];
                if (topPage is NavigationPage navPage)
                {
                    if (navPage.CurrentPage is ViewPage viewPage)
                    {
                        lastPageBeforeLock = new PreviousPageInfo
                        {
                            Page = "view",
                            CipherId = viewPage.ViewModel.CipherId
                        };
                    }
                    else if (navPage.CurrentPage is AddEditPage addEditPage && addEditPage.ViewModel.EditMode)
                    {
                        lastPageBeforeLock = new PreviousPageInfo
                        {
                            Page = "edit",
                            CipherId = addEditPage.ViewModel.CipherId
                        };
                    }
                }
            }
            await _stateService.SetPreviousPageInfoAsync(lastPageBeforeLock);
            var lockPage = new LockPage(Options, autoPromptBiometric);
            Device.BeginInvokeOnMainThread(() => Current.MainPage = new NavigationPage(lockPage));
        }
    }
}
