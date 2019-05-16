using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICryptoService _cryptoService;
        private readonly IUserService _userService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IEnvironmentService _environmentService;
        private readonly IMessagingService _messagingService;
        private readonly ILockService _lockService;
        private readonly IStorageService _storageService;
        private readonly ISyncService _syncService;

        private bool _pin;
        private bool _fingerprint;
        private string _lastSyncDate;
        private string _lockOptionValue;
        private List<KeyValuePair<string, int?>> _lockOptions =
            new List<KeyValuePair<string, int?>>
            {
                new KeyValuePair<string, int?>(AppResources.LockOptionImmediately, 0),
                new KeyValuePair<string, int?>(AppResources.LockOption1Minute, 1),
                new KeyValuePair<string, int?>(AppResources.LockOption5Minutes, 5),
                new KeyValuePair<string, int?>(AppResources.LockOption15Minutes, 15),
                new KeyValuePair<string, int?>(AppResources.LockOption30Minutes, 30),
                new KeyValuePair<string, int?>(AppResources.LockOption1Hour, 60),
                new KeyValuePair<string, int?>(AppResources.LockOption4Hours, 240),
                new KeyValuePair<string, int?>(AppResources.Never, null),
            };

        public SettingsPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _lockService = ServiceContainer.Resolve<ILockService>("lockService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            GroupedItems = new ExtendedObservableCollection<SettingsPageListGroup>();
            PageTitle = AppResources.Settings;
        }

        public ExtendedObservableCollection<SettingsPageListGroup> GroupedItems { get; set; }

        public async Task InitAsync()
        {
            var lastSync = await _syncService.GetLastSyncAsync();
            if(lastSync != null)
            {
                _lastSyncDate = string.Format("{0} {1}", lastSync.Value.ToShortDateString(),
                    lastSync.Value.ToShortTimeString());
            }
            var option = await _storageService.GetAsync<int?>(Constants.LockOptionKey);
            _lockOptionValue = _lockOptions.FirstOrDefault(o => o.Value == option).Key;
            var pinSet = await _lockService.IsPinLockSetAsync();
            _pin = pinSet.Item1 || pinSet.Item2;
            // TODO: Fingerprint
            BuildList();
        }

        public async Task AboutAsync()
        {
            var debugText = string.Format("{0}: {1}", AppResources.Version,
                _platformUtilsService.GetApplicationVersion());
            var text = string.Format("© 8bit Solutions LLC 2015-{0}\n\n{1}", DateTime.Now.Year, debugText);
            var copy = await _platformUtilsService.ShowDialogAsync(text, AppResources.Bitwarden, AppResources.Copy,
                AppResources.Close);
            if(copy)
            {
                await _platformUtilsService.CopyToClipboardAsync(debugText);
            }
        }

        public void Help()
        {
            _platformUtilsService.LaunchUri("https://help.bitwarden.com/");
        }

        public async Task FingerprintAsync()
        {
            var fingerprint = await _cryptoService.GetFingerprintAsync(await _userService.GetUserIdAsync());
            var phrase = string.Join("-", fingerprint);
            var text = string.Format("{0}:\n\n{1}", AppResources.YourAccountsFingerprint, phrase);
            var learnMore = await _platformUtilsService.ShowDialogAsync(text, AppResources.FingerprintPhrase,
                AppResources.LearnMore, AppResources.Close);
            if(learnMore)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/fingerprint-phrase/");
            }
        }

        public void Rate()
        {
            _deviceActionService.RateApp();
        }

        public void Import()
        {
            _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/import-data/");
        }

        public void Export()
        {
            _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/export-your-data/");
        }

        public void WebVault()
        {
            var url = _environmentService.GetWebVaultUrl();
            if(url == null)
            {
                url = "https://vault.bitwarden.com";
            }
            _platformUtilsService.LaunchUri(url);
        }

        public async Task ShareAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.ShareVaultConfirmation,
                AppResources.ShareVault, AppResources.Yes, AppResources.Cancel);
            if(confirmed)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/what-is-an-organization/");
            }
        }

        public async Task TwoStepAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.TwoStepLoginConfirmation,
                AppResources.TwoStepLogin, AppResources.Yes, AppResources.Cancel);
            if(confirmed)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/setup-two-step-login/");
            }
        }

        public async Task ChangePasswordAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.ChangePasswordConfirmation,
                AppResources.ChangeMasterPassword, AppResources.Yes, AppResources.Cancel);
            if(confirmed)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/change-your-master-password/");
            }
        }

        public async Task LogOutAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
            if(confirmed)
            {
                _messagingService.Send("logout");
            }
        }

        public async Task LockAsync()
        {
            await _lockService.LockAsync(true);
        }

        public async Task LockOptionsAsync()
        {
            var options = _lockOptions.Select(o => o.Key == _lockOptionValue ? $"✓ {o.Key}" : o.Key).ToArray();
            var selection = await Page.DisplayActionSheet(AppResources.LockOptions, AppResources.Cancel, null, options);
            if(selection == AppResources.Cancel)
            {
                return;
            }
            var cleanSelection = selection.Replace("✓ ", string.Empty);
            var selectionOption = _lockOptions.FirstOrDefault(o => o.Key == cleanSelection);
            _lockOptionValue = selectionOption.Key;
            await _storageService.SaveAsync(Constants.LockOptionKey, selectionOption.Value);
            BuildList();
        }

        private void BuildList()
        {
            var doUpper = Device.RuntimePlatform != Device.Android;
            var manageItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.Folders },
                new SettingsPageListItem { Name = AppResources.Sync, SubLabel = _lastSyncDate }
            };
            var securityItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.LockOptions, SubLabel = _lockOptionValue },
                new SettingsPageListItem { Name = string.Format(AppResources.UnlockWith, AppResources.Fingerprint) },
                new SettingsPageListItem { Name = AppResources.UnlockWithPIN },
                new SettingsPageListItem { Name = AppResources.LockNow },
                new SettingsPageListItem { Name = AppResources.TwoStepLogin }
            };
            var accountItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.ChangeMasterPassword },
                new SettingsPageListItem { Name = AppResources.FingerprintPhrase },
                new SettingsPageListItem { Name = AppResources.LogOut }
            };
            var toolsItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.ImportItems },
                new SettingsPageListItem { Name = AppResources.ExportVault },
                new SettingsPageListItem { Name = AppResources.ShareVault },
                new SettingsPageListItem { Name = AppResources.WebVault }
            };
            var otherItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.Options },
                new SettingsPageListItem { Name = AppResources.About },
                new SettingsPageListItem { Name = AppResources.HelpAndFeedback },
                new SettingsPageListItem { Name = AppResources.RateTheApp }
            };
            GroupedItems.ResetWithRange(new List<SettingsPageListGroup>
            {
                new SettingsPageListGroup(manageItems, AppResources.Manage, doUpper),
                new SettingsPageListGroup(securityItems, AppResources.Security, doUpper),
                new SettingsPageListGroup(accountItems, AppResources.Account, doUpper),
                new SettingsPageListGroup(toolsItems, AppResources.Tools, doUpper),
                new SettingsPageListGroup(otherItems, AppResources.Other, doUpper)
            });
        }
    }
}
