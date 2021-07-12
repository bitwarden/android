using System;
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
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class AppHelpers
    {
        public static async Task<string> CipherListOptions(ContentPage page, CipherView cipher, IPasswordRepromptService passwordRepromptService)
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
                if (cipher.Reprompt == CipherRepromptType.None || await passwordRepromptService.ShowPasswordPromptAsync())
                {
                    await page.Navigation.PushModalAsync(new NavigationPage(new AddEditPage(cipher.Id)));
                }
            }
            else if (selection == AppResources.CopyUsername)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Login.Username);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Username));
            }
            else if (selection == AppResources.CopyPassword)
            {
                if (cipher.Reprompt == CipherRepromptType.None || await passwordRepromptService.ShowPasswordPromptAsync())
                {
                    await platformUtilsService.CopyToClipboardAsync(cipher.Login.Password);
                    platformUtilsService.ShowToast("info", null,
                        string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
                    var task = eventService.CollectAsync(Core.Enums.EventType.Cipher_ClientCopiedPassword, cipher.Id);
                }
            }
            else if (selection == AppResources.CopyTotp)
            {
                if (cipher.Reprompt == CipherRepromptType.None || await passwordRepromptService.ShowPasswordPromptAsync())
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
            }
            else if (selection == AppResources.Launch)
            {
                platformUtilsService.LaunchUri(cipher.Login.LaunchUri);
            }
            else if (selection == AppResources.CopyNumber)
            {
                if (cipher.Reprompt == CipherRepromptType.None || await passwordRepromptService.ShowPasswordPromptAsync())
                {
                    await platformUtilsService.CopyToClipboardAsync(cipher.Card.Number);
                    platformUtilsService.ShowToast("info", null,
                        string.Format(AppResources.ValueHasBeenCopied, AppResources.Number));
                }
            }
            else if (selection == AppResources.CopySecurityCode)
            {
                if (cipher.Reprompt == CipherRepromptType.None || await passwordRepromptService.ShowPasswordPromptAsync())
                {
                    await platformUtilsService.CopyToClipboardAsync(cipher.Card.Code);
                    platformUtilsService.ShowToast("info", null,
                        string.Format(AppResources.ValueHasBeenCopied, AppResources.SecurityCode));
                    var task = eventService.CollectAsync(Core.Enums.EventType.Cipher_ClientCopiedCardCode, cipher.Id);
                }
            }
            else if (selection == AppResources.CopyNotes)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Notes);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Notes));
            }
            return selection;
        }

        public static async Task<string> SendListOptions(ContentPage page, SendView send)
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            var options = new List<string> { AppResources.Edit };
            options.Add(AppResources.CopyLink);
            options.Add(AppResources.ShareLink);
            if (send.HasPassword)
            {
                options.Add(AppResources.RemovePassword);
            }

            var selection = await page.DisplayActionSheet(send.Name, AppResources.Cancel, AppResources.Delete,
                options.ToArray());
            if (await vaultTimeoutService.IsLockedAsync())
            {
                platformUtilsService.ShowToast("info", null, AppResources.VaultIsLocked);
            }
            else if (selection == AppResources.Edit)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new SendAddEditPage(null, send.Id)));
            }
            else if (selection == AppResources.CopyLink)
            {
                await CopySendUrlAsync(send);
            }
            else if (selection == AppResources.ShareLink)
            {
                await ShareSendUrlAsync(send);
            }
            else if (selection == AppResources.RemovePassword)
            {
                await RemoveSendPasswordAsync(send.Id);
            }
            else if (selection == AppResources.Delete)
            {
                await DeleteSendAsync(send.Id);
            }
            return selection;
        }

        public static async Task CopySendUrlAsync(SendView send)
        {
            if (await IsSendDisabledByPolicyAsync())
            {
                return;
            }
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            await platformUtilsService.CopyToClipboardAsync(GetSendUrl(send));
            platformUtilsService.ShowToast("info", null,
                string.Format(AppResources.ValueHasBeenCopied, AppResources.SendLink));
        }

        public static async Task ShareSendUrlAsync(SendView send)
        {
            if (await IsSendDisabledByPolicyAsync())
            {
                return;
            }
            await Share.RequestAsync(new ShareTextRequest
            {
                Uri = new Uri(GetSendUrl(send)).ToString(),
                Title = AppResources.Send + " " + send.Name,
                Subject = send.Name
            });
        }
        
        private static string GetSendUrl(SendView send)
        {
            var environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            var webVaultUrl = environmentService.GetWebVaultUrl();
            if (webVaultUrl != null)
            {
                return webVaultUrl + "/#/send/" + send.AccessId + "/" + send.UrlB64Key;
            }
            return "https://send.bitwarden.com/#" + send.AccessId + "/" + send.UrlB64Key;
        }

        public static async Task<bool> RemoveSendPasswordAsync(string sendId)
        {
            if (await IsSendDisabledByPolicyAsync())
            {
                return false;
            }
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            var sendService = ServiceContainer.Resolve<ISendService>("sendService");

            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            var confirmed = await platformUtilsService.ShowDialogAsync(
                AppResources.AreYouSureRemoveSendPassword,
                null, AppResources.Yes, AppResources.Cancel);
            if (!confirmed)
            {
                return false;
            }
            try
            {
                await deviceActionService.ShowLoadingAsync(AppResources.RemovingSendPassword);
                await sendService.RemovePasswordWithServerAsync(sendId);
                await deviceActionService.HideLoadingAsync();
                platformUtilsService.ShowToast("success", null, AppResources.SendPasswordRemoved);
                return true;
            }
            catch (ApiException e)
            {
                await deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            return false;
        }

        public static async Task<bool> DeleteSendAsync(string sendId)
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            var sendService = ServiceContainer.Resolve<ISendService>("sendService");

            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            var confirmed = await platformUtilsService.ShowDialogAsync(
                AppResources.AreYouSureDeleteSend,
                null, AppResources.Yes, AppResources.Cancel);
            if (!confirmed)
            {
                return false;
            }
            try
            {
                await deviceActionService.ShowLoadingAsync(AppResources.Deleting);
                await sendService.DeleteWithServerAsync(sendId);
                await deviceActionService.HideLoadingAsync();
                platformUtilsService.ShowToast("success", null, AppResources.SendDeleted);
                return true;
            }
            catch (ApiException e)
            {
                await deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            return false;
        }

        public static async Task<bool> IsSendDisabledByPolicyAsync()
        {
            var policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            var userService = ServiceContainer.Resolve<IUserService>("userService");
            
            var policies = await policyService.GetAll(PolicyType.DisableSend);
            var organizations = await userService.GetAllOrganizationAsync();
            return organizations.Any(o =>
            {
                return o.Enabled &&
                       o.Status == OrganizationUserStatusType.Confirmed &&
                       o.UsePolicies &&
                       !o.canManagePolicies &&
                       policies.Any(p => p.OrganizationId == o.Id && p.Enabled);
            });
        }

        public static async Task<bool> IsHideEmailDisabledByPolicyAsync()
        {
            var policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            var userService = ServiceContainer.Resolve<IUserService>("userService");

            var policies = await policyService.GetAll(PolicyType.SendOptions);
            var organizations = await userService.GetAllOrganizationAsync();
            return organizations.Any(o =>
            {
                return o.Enabled &&
                       o.Status == OrganizationUserStatusType.Confirmed &&
                       o.UsePolicies &&
                       !o.canManagePolicies &&
                       policies.Any(p => p.OrganizationId == o.Id &&
                            p.Enabled &&
                            p.Data.ContainsKey("disableHideEmail") &&
                            (bool)p.Data["disableHideEmail"]);
            });
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
                if (appOptions.CreateSend != null)
                {
                    Application.Current.MainPage = new NavigationPage(new SendAddEditPage(appOptions));
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

        public static async Task<int> IncrementInvalidUnlockAttemptsAsync()
        {
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var invalidUnlockAttempts = await storageService.GetAsync<int>(Constants.InvalidUnlockAttempts); 
            invalidUnlockAttempts++;
            await storageService.SaveAsync(Constants.InvalidUnlockAttempts, invalidUnlockAttempts);
            return invalidUnlockAttempts;
        }
        
        public static async Task ResetInvalidUnlockAttemptsAsync()
        {
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            await storageService.RemoveAsync(Constants.InvalidUnlockAttempts);
        }

        public static async Task<bool> IsVaultTimeoutImmediateAsync()
        {
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            
            var vaultTimeoutMinutes = await storageService.GetAsync<int?>(Constants.VaultTimeoutKey);
            if (vaultTimeoutMinutes.GetValueOrDefault(-1) == 0)
            {
                return true;
            }
            return false;
        }
    }
}
