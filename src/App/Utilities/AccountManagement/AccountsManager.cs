using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Utilities.AccountManagement
{
    public class AccountsManager : IAccountsManager
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IStorageService _secureStorageService;
        private readonly IStateService _stateService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IAuthService _authService;
        private readonly ILogger _logger;
        private readonly IMessagingService _messagingService;
        Func<AppOptions> _getOptionsFunc;
        private IAccountsManagerHost _accountsManagerHost;

        public AccountsManager(IBroadcasterService broadcasterService,
                               IVaultTimeoutService vaultTimeoutService,
                               IStorageService secureStorageService,
                               IStateService stateService,
                               IPlatformUtilsService platformUtilsService,
                               IAuthService authService,
                               ILogger logger,
                               IMessagingService messagingService)
        {
            _broadcasterService = broadcasterService;
            _vaultTimeoutService = vaultTimeoutService;
            _secureStorageService = secureStorageService;
            _stateService = stateService;
            _platformUtilsService = platformUtilsService;
            _authService = authService;
            _logger = logger;
            _messagingService = messagingService;
        }

        private AppOptions Options => _getOptionsFunc?.Invoke() ?? new AppOptions { IosExtension = true };

        public void Init(Func<AppOptions> getOptionsFunc, IAccountsManagerHost accountsManagerHost)
        {
            _getOptionsFunc = getOptionsFunc;
            _accountsManagerHost = accountsManagerHost;

            _broadcasterService.Subscribe(nameof(AccountsManager), OnMessage);
        }

        public async Task NavigateOnAccountChangeAsync(bool? isAuthed = null)
        {
            // TODO: this could be improved by doing chain of responsability pattern
            // but for now it may be an overkill, if logic gets more complex consider refactoring it

            var authed = isAuthed ?? await _stateService.IsAuthenticatedAsync();
            if (authed)
            {
                if (await _vaultTimeoutService.IsLoggedOutByTimeoutAsync() ||
                    await _vaultTimeoutService.ShouldLogOutByTimeoutAsync())
                {
                    // TODO implement orgIdentifier flow to SSO Login page, same as email flow below
                    // var orgIdentifier = await _stateService.GetOrgIdentifierAsync();

                    var email = await _stateService.GetEmailAsync();
                    Options.HideAccountSwitcher = await _stateService.GetActiveUserIdAsync() == null;
                    _accountsManagerHost.Navigate(NavigationTarget.Login, new LoginNavigationParams(email));
                }
                else if (await _vaultTimeoutService.IsLockedAsync() ||
                         await _vaultTimeoutService.ShouldLockAsync())
                {
                    _accountsManagerHost.Navigate(NavigationTarget.Lock);
                }
                else if (Options.FromAutofillFramework && Options.SaveType.HasValue)
                {
                    _accountsManagerHost.Navigate(NavigationTarget.AddEditCipher);
                }
                else if (Options.Uri != null)
                {
                    _accountsManagerHost.Navigate(NavigationTarget.AutofillCiphers);
                }
                else if (Options.CreateSend != null)
                {
                    _accountsManagerHost.Navigate(NavigationTarget.SendAddEdit);
                }
                else
                {
                    _accountsManagerHost.Navigate(NavigationTarget.Home);
                }
            }
            else
            {
                Options.HideAccountSwitcher = await _stateService.GetActiveUserIdAsync() == null;
                if (await _vaultTimeoutService.IsLoggedOutByTimeoutAsync() ||
                    await _vaultTimeoutService.ShouldLogOutByTimeoutAsync())
                {
                    // TODO implement orgIdentifier flow to SSO Login page, same as email flow below
                    // var orgIdentifier = await _stateService.GetOrgIdentifierAsync();

                    var email = await _stateService.GetEmailAsync();
                    await _stateService.SetRememberedEmailAsync(email);
                    _accountsManagerHost.Navigate(NavigationTarget.HomeLogin, new HomeNavigationParams(true));
                }
                else
                {
                    _accountsManagerHost.Navigate(NavigationTarget.HomeLogin, new HomeNavigationParams(false));
                }
            }
        }

        private async void OnMessage(Message message)
        {
            try
            {
                switch (message.Command)
                {
                    case AccountsManagerMessageCommands.LOCKED:
                        await Device.InvokeOnMainThreadAsync(() => LockedAsync(message.Data as Tuple<string, bool>));
                        break;
                    case AccountsManagerMessageCommands.LOCK_VAULT:
                        await _vaultTimeoutService.LockAsync(true);
                        break;
                    case AccountsManagerMessageCommands.LOGOUT:
                        var extras = message.Data as Tuple<string, bool, bool>;
                        var userId = extras?.Item1;
                        var userInitiated = extras?.Item2 ?? true;
                        var expired = extras?.Item3 ?? false;
                        await Device.InvokeOnMainThreadAsync(() => LogOutAsync(userId, userInitiated, expired));
                        break;
                    case AccountsManagerMessageCommands.LOGGED_OUT:
                        // Clean up old migrated key if they ever log out.
                        await _secureStorageService.RemoveAsync("oldKey");
                        break;
                    case AccountsManagerMessageCommands.ADD_ACCOUNT:
                        await AddAccountAsync();
                        break;
                    case AccountsManagerMessageCommands.ACCOUNT_ADDED:
                        await _accountsManagerHost.UpdateThemeAsync();
                        break;
                    case AccountsManagerMessageCommands.SWITCHED_ACCOUNT:
                        await SwitchedAccountAsync();
                        break;
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private async Task LockedAsync(Tuple<string, bool> extras)
        {
            var userId = extras?.Item1;
            var userInitiated = extras?.Item2 ?? false;

            if (!await _stateService.IsActiveAccountAsync(userId))
            {
                _platformUtilsService.ShowToast("info", null, AppResources.AccountLockedSuccessfully);
                return;
            }

            var autoPromptBiometric = !userInitiated;
            if (autoPromptBiometric && Device.RuntimePlatform == Device.iOS)
            {
                var vaultTimeout = await _stateService.GetVaultTimeoutAsync();
                if (vaultTimeout == 0)
                {
                    autoPromptBiometric = false;
                }
            }

            await _accountsManagerHost.SetPreviousPageInfoAsync();

            await Device.InvokeOnMainThreadAsync(() => _accountsManagerHost.Navigate(NavigationTarget.Lock, new LockNavigationParams(autoPromptBiometric)));
        }

        private async Task AddAccountAsync()
        {
            await Device.InvokeOnMainThreadAsync(() =>
            {
                Options.HideAccountSwitcher = false;
                _accountsManagerHost.Navigate(NavigationTarget.HomeLogin, new HomeNavigationParams(false));
            });
        }

        public async Task LogOutAsync(string userId, bool userInitiated, bool expired)
        {
            await AppHelpers.LogOutAsync(userId, userInitiated);
            await NavigateOnAccountChangeAsync();
            _authService.LogOut(() =>
            {
                if (expired)
                {
                    _platformUtilsService.ShowToast("warning", null, AppResources.LoginExpired);
                }
            });
        }

        private async Task SwitchedAccountAsync()
        {
            await AppHelpers.OnAccountSwitchAsync();
            await Device.InvokeOnMainThreadAsync(async () =>
            {
                if (await _vaultTimeoutService.ShouldTimeoutAsync())
                {
                    await _vaultTimeoutService.ExecuteTimeoutActionAsync();
                }
                else
                {
                    await NavigateOnAccountChangeAsync();
                }
                await Task.Delay(50);
                await _accountsManagerHost.UpdateThemeAsync();
                _messagingService.Send(AccountsManagerMessageCommands.ACCOUNT_SWITCH_COMPLETED);
            });
        }

        public async Task PromptToSwitchToExistingAccountAsync(string userId)
        {
            var switchToAccount = await _platformUtilsService.ShowDialogAsync(
                AppResources.SwitchToAlreadyAddedAccountConfirmation,
                AppResources.AccountAlreadyAdded, AppResources.Yes, AppResources.Cancel);
            if (switchToAccount)
            {
                await _stateService.SetActiveUserAsync(userId);
                _messagingService.Send("switchedAccount");
            }
        }
    }
}
