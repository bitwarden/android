using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Models;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class AppHelpers
    {
        public static async Task<string> CipherListOptions(ContentPage page, CipherView cipher)
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var eventService = ServiceContainer.Resolve<IEventService>("eventService");
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            var options = new List<string> { AppResources.View };
            if (!cipher.IsDeleted)
            {
                options.Add(AppResources.Edit);
            }
            if (cipher.Type == Core.Enums.CipherType.Login)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Login.Username))
                {
                    options.Add(AppResources.CopyUsername);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Login.Password) && cipher.ViewPassword)
                {
                    options.Add(AppResources.CopyPassword);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Login.Totp))
                {
                    var userService = ServiceContainer.Resolve<IUserService>("userService");
                    var canAccessPremium = await userService.CanAccessPremiumAsync();
                    if (canAccessPremium || cipher.OrganizationUseTotp)
                    {
                        options.Add(AppResources.CopyTotp);
                    }
                }
                if (cipher.Login.CanLaunch)
                {
                    options.Add(AppResources.Launch);
                }
            }
            else if (cipher.Type == Core.Enums.CipherType.Card)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Card.Number))
                {
                    options.Add(AppResources.CopyNumber);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Card.Code))
                {
                    options.Add(AppResources.CopySecurityCode);
                }
            }
            else if (cipher.Type == Core.Enums.CipherType.SecureNote)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Notes))
                {
                    options.Add(AppResources.CopyNotes);
                }
            }
            var selection = await page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, options.ToArray());
            if (await vaultTimeoutService.IsLockedAsync())
            {
                platformUtilsService.ShowToast("info", null, AppResources.VaultIsLocked);
            }
            else if (selection == AppResources.View)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new ViewPage(cipher.Id)));
            }
            else if (selection == AppResources.Edit)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new AddEditPage(cipher.Id)));
            }
            else if (selection == AppResources.CopyUsername)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Login.Username);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Username));
            }
            else if (selection == AppResources.CopyPassword)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Login.Password);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
                var task = eventService.CollectAsync(Core.Enums.EventType.Cipher_ClientCopiedPassword, cipher.Id);
            }
            else if (selection == AppResources.CopyTotp)
            {
                var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                var totp = await totpService.GetCodeAsync(cipher.Login.Totp);
                if (!string.IsNullOrWhiteSpace(totp))
                {
                    await platformUtilsService.CopyToClipboardAsync(totp);
                    platformUtilsService.ShowToast("info", null,
                        string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
                }
            }
            else if (selection == AppResources.Launch)
            {
                platformUtilsService.LaunchUri(cipher.Login.LaunchUri);
            }
            else if (selection == AppResources.CopyNumber)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Card.Number);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Number));
            }
            else if (selection == AppResources.CopySecurityCode)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Card.Code);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.SecurityCode));
                var task = eventService.CollectAsync(Core.Enums.EventType.Cipher_ClientCopiedCardCode, cipher.Id);
            }
            else if (selection == AppResources.CopyNotes)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Notes);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Notes));
            }
            return selection;
        }

        public static async Task<bool> PerformUpdateTasksAsync(ISyncService syncService,
            IDeviceActionService deviceActionService, IStorageService storageService)
        {
            var currentBuild = deviceActionService.GetBuildNumber();
            var lastBuild = await storageService.GetAsync<string>(Constants.LastBuildKey);
            if (lastBuild == null)
            {
                // Installed
                var currentTimeout = await storageService.GetAsync<int?>(Constants.VaultTimeoutKey);
                if (currentTimeout == null)
                {
                    await storageService.SaveAsync(Constants.VaultTimeoutKey, 15);
                }
                
                var currentAction = await storageService.GetAsync<string>(Constants.VaultTimeoutActionKey);
                if (currentAction == null)
                {
                    await storageService.SaveAsync(Constants.VaultTimeoutActionKey, "lock");
                }
            }
            else if (lastBuild != currentBuild)
            {
                // Updated
                var tasks = Task.Run(() => syncService.FullSyncAsync(true));
            }
            if (lastBuild != currentBuild)
            {
                await storageService.SaveAsync(Constants.LastBuildKey, currentBuild);
                return true;
            }
            return false;
        }

        public static async Task SetPreconfiguredSettingsAsync(IDictionary<string, string> configSettings)
        {
            if (configSettings?.Any() ?? true)
            {
                return;
            }
            foreach (var setting in configSettings)
            {
                switch (setting.Key)
                {
                    case "baseEnvironmentUrl":
                        var environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
                        var settingValue = string.IsNullOrWhiteSpace(setting.Value) ? null : setting.Value;
                        if (environmentService.BaseUrl != settingValue)
                        {
                            await environmentService.SetUrlsAsync(new Core.Models.Data.EnvironmentUrlData
                            {
                                Base = settingValue,
                                Api = environmentService.ApiUrl,
                                Identity = environmentService.IdentityUrl,
                                WebVault = environmentService.WebVaultUrl,
                                Icons = environmentService.IconsUrl
                            });
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        public static bool SetAlternateMainPage(AppOptions appOptions)
        {
            if (appOptions != null)
            {
                if (appOptions.FromAutofillFramework && appOptions.SaveType.HasValue)
                {
                    Application.Current.MainPage = new NavigationPage(new AddEditPage(appOptions: appOptions));
                    return true;
                }
                if (appOptions.Uri != null)
                {
                    Application.Current.MainPage = new NavigationPage(new AutofillCiphersPage(appOptions));
                    return true;
                }
            }
            return false;
        }

        public static async Task<PreviousPageInfo> ClearPreviousPage()
        {
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var previousPage = await storageService.GetAsync<PreviousPageInfo>(Constants.PreviousPageKey);
            if (previousPage != null)
            {
                await storageService.RemoveAsync(Constants.PreviousPageKey);
            }
            return previousPage;
        }
    }
}
