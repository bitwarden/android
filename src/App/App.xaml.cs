using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
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
        private readonly MobileI18nService _i18nService;
        private readonly IUserService _userService;
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly IStateService _stateService;
        private readonly ILockService _lockService;
        private readonly ISyncService _syncService;
        private readonly ITokenService _tokenService;
        private readonly ICryptoService _cryptoService;
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly ISettingsService _settingsService;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly ISearchService _searchService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IAuthService _authService;
        private readonly IStorageService _storageService;

        public App()
        {
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _lockService = ServiceContainer.Resolve<ILockService>("lockService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _tokenService = ServiceContainer.Resolve<ITokenService>("tokenService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _settingsService = ServiceContainer.Resolve<ISettingsService>("settingsService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService") as MobileI18nService;

            InitializeComponent();
            SetCulture();
            ThemeManager.SetThemeStyle("light");
            MainPage = new HomePage();
            var mainPageTask = SetMainPageAsync();

            ServiceContainer.Resolve<MobilePlatformUtilsService>("platformUtilsService").Init();
            _broadcasterService.Subscribe(nameof(App), async (message) =>
            {
                if(message.Command == "showDialog")
                {
                    var details = message.Data as DialogDetails;
                    var confirmed = true;
                    var confirmText = string.IsNullOrWhiteSpace(details.ConfirmText) ?
                        AppResources.Ok : details.ConfirmText;
                    if(!string.IsNullOrWhiteSpace(details.CancelText))
                    {
                        confirmed = await MainPage.DisplayAlert(details.Title, details.Text, confirmText,
                            details.CancelText);
                    }
                    else
                    {
                        await MainPage.DisplayAlert(details.Title, details.Text, confirmText);
                    }
                    _messagingService.Send("showDialogResolve", new Tuple<int, bool>(details.DialogId, confirmed));
                }
                else if(message.Command == "locked")
                {
                    await _stateService.PurgeAsync();
                    MainPage = new NavigationPage(new LockPage());
                }
                else if(message.Command == "lockVault")
                {
                    await _lockService.LockAsync(true);
                }
                else if(message.Command == "logout")
                {
                    await LogOutAsync(false);
                }
                else if(message.Command == "loggedOut")
                {
                    // TODO
                }
                else if(message.Command == "unlocked" || message.Command == "loggedIn")
                {
                    // TODO
                }
            });
        }

        protected override void OnStart()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnStart");
        }

        protected async override void OnSleep()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnSleep");
            await HandleLockingAsync();
        }

        protected override void OnResume()
        {
            System.Diagnostics.Debug.WriteLine("XF App: OnResume");
            _messagingService.Send("cancelLockTimer");
        }

        private void SetCulture()
        {
            // Calendars are removed by linker. ref https://bugzilla.xamarin.com/show_bug.cgi?id=59077
            new System.Globalization.ThaiBuddhistCalendar();
            new System.Globalization.HijriCalendar();
            new System.Globalization.UmAlQuraCalendar();
        }

        private async Task LogOutAsync(bool expired)
        {
            var userId = await _userService.GetUserIdAsync();
            await Task.WhenAll(
                _syncService.SetLastSyncAsync(DateTime.MinValue),
                _tokenService.ClearTokenAsync(),
                _cryptoService.ClearKeysAsync(),
                _userService.ClearAsync(),
                _settingsService.ClearAsync(userId),
                _cipherService.ClearAsync(userId),
                _folderService.ClearAsync(userId),
                _collectionService.ClearAsync(userId),
                _passwordGenerationService.ClearAsync(),
                _lockService.ClearAsync());
            _lockService.PinLocked = false;
            _searchService.ClearIndex();
            _authService.LogOut(() =>
            {
                if(expired)
                {
                    // TODO: Toast?
                }
                MainPage = new HomePage();
            });
        }

        private async Task SetMainPageAsync()
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if(authed)
            {
                var locked = await _lockService.IsLockedAsync();
                if(locked)
                {
                    Current.MainPage = new NavigationPage(new LockPage());
                }
                else
                {
                    Current.MainPage = new TabsPage();
                }
            }
            else
            {
                Current.MainPage = new HomePage();
            }
        }

        private async Task HandleLockingAsync()
        {
            var lockOption = _platformUtilsService.LockTimeout();
            if(lockOption == null)
            {
                lockOption = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            }
            lockOption = lockOption.GetValueOrDefault(-1);
            if(lockOption > 0)
            {
                _messagingService.Send("scheduleLockTimer", lockOption.Value);
            }
            else if(lockOption == 0)
            {
                await _lockService.LockAsync(true);
            }
        }
    }
}
