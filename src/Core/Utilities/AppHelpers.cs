using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Microsoft.Maui.Networking;
using Microsoft.Maui.ApplicationModel.DataTransfer;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using NetworkAccess = Microsoft.Maui.Networking.NetworkAccess;

namespace Bit.App.Utilities
{
    public static class AppHelpers
    {
        public const string VAULT_TIMEOUT_ACTION_CHANGED_MESSAGE_COMMAND = "vaultTimeoutActionChanged";
        public const string RESUMED_MESSAGE_COMMAND = "resumed";

        public static async Task<string> CipherListOptions(ContentPage page, CipherView cipher, IPasswordRepromptService passwordRepromptService)
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var eventService = ServiceContainer.Resolve<IEventService>("eventService");
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            var clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");

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
                    var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                    var canAccessPremium = await stateService.CanAccessPremiumAsync();
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
                await page.Navigation.PushModalAsync(new NavigationPage(new CipherDetailsPage(cipher.Id)));
            }
            else if (selection == AppResources.Edit
                     &&
                     await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new CipherAddEditPage(cipher.Id)));
            }
            else if (selection == AppResources.CopyUsername)
            {
                await clipboardService.CopyTextAsync(cipher.Login.Username);
                platformUtilsService.ShowToastForCopiedValue(AppResources.Username);
            }
            else if (selection == AppResources.CopyPassword
                     &&
                     await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                await clipboardService.CopyTextAsync(cipher.Login.Password);
                platformUtilsService.ShowToastForCopiedValue(AppResources.Password);
                var task = eventService.CollectAsync(Core.Enums.EventType.Cipher_ClientCopiedPassword, cipher.Id);
            }
            else if (selection == AppResources.CopyTotp
                     &&
                     await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                var totp = await totpService.GetCodeAsync(cipher.Login.Totp);
                if (!string.IsNullOrWhiteSpace(totp))
                {
                    await clipboardService.CopyTextAsync(totp);
                    platformUtilsService.ShowToastForCopiedValue(AppResources.VerificationCodeTotp);
                }
            }
            else if (selection == AppResources.Launch && cipher.CanLaunch)
            {
                platformUtilsService.LaunchUri(cipher.LaunchUri);
            }
            else if (selection == AppResources.CopyNumber
                     &&
                     await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                await clipboardService.CopyTextAsync(cipher.Card.Number);
                platformUtilsService.ShowToastForCopiedValue(AppResources.Number);
            }
            else if (selection == AppResources.CopySecurityCode
                     &&
                     await passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                await clipboardService.CopyTextAsync(cipher.Card.Code);
                platformUtilsService.ShowToastForCopiedValue(AppResources.SecurityCode);
                eventService.CollectAsync(EventType.Cipher_ClientCopiedCardCode, cipher.Id).FireAndForget();
            }
            else if (selection == AppResources.CopyNotes)
            {
                await clipboardService.CopyTextAsync(cipher.Notes);
                platformUtilsService.ShowToastForCopiedValue(AppResources.Notes);
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

        public static async Task<string> AccountListOptions(ContentPage page, AccountViewCellViewModel accountViewCell)
        {
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");

            var userId = accountViewCell.AccountView.UserId;

            List<string> options;
            if (await vaultTimeoutService.IsLoggedOutByTimeoutAsync(userId) ||
                await vaultTimeoutService.ShouldLogOutByTimeoutAsync(userId))
            {
                options = new List<string> { AppResources.RemoveAccount };
            }
            else if (await vaultTimeoutService.IsLockedAsync(userId) ||
                     await vaultTimeoutService.ShouldLockAsync(userId))
            {
                options = new List<string> { AppResources.LogOut };
            }
            else
            {
                options = new List<string> { AppResources.Lock, AppResources.LogOut };
            }

            var accountSummary = accountViewCell.AccountView.Email;
            if (!string.IsNullOrWhiteSpace(accountViewCell.AccountView.Hostname))
            {
                accountSummary += "\n" + accountViewCell.AccountView.Hostname;
            }
            var selection = await page.DisplayActionSheet(accountSummary, AppResources.Cancel, null, options.ToArray());

            if (selection == AppResources.Lock)
            {
                await vaultTimeoutService.LockAsync(true, true, userId);
            }
            else if (selection == AppResources.LogOut || selection == AppResources.RemoveAccount)
            {
                var title = selection == AppResources.LogOut ? AppResources.LogOut : AppResources.RemoveAccount;
                var text = (selection == AppResources.LogOut ? AppResources.LogoutConfirmation
                    : AppResources.RemoveAccountConfirmation) + "\n\n" + accountSummary;
                var confirmed =
                    await platformUtilsService.ShowDialogAsync(text, title, AppResources.Yes, AppResources.Cancel);
                if (confirmed)
                {
                    var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                    if (await stateService.IsActiveAccountAsync(userId))
                    {
                        var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
                        messagingService.Send("logout");
                        return selection;
                    }
                    await LogOutAsync(userId, true);
                }
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
            var clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");
            await clipboardService.CopyTextAsync(GetSendUrl(send));
            platformUtilsService.ShowToastForCopiedValue(AppResources.SendLink);
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
            return environmentService.GetWebSendUrl() + send.AccessId + "/" + send.UrlB64Key;
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
            return await policyService.PolicyAppliesToUser(PolicyType.DisableSend);
        }

        public static async Task<bool> IsHideEmailDisabledByPolicyAsync()
        {
            var policyService = ServiceContainer.Resolve<IPolicyService>("policyService");

            return await policyService.PolicyAppliesToUser(PolicyType.SendOptions,
                policy => policy.Data.ContainsKey("disableHideEmail") && (bool)policy.Data["disableHideEmail"]);
        }

        public static async Task<bool> PerformUpdateTasksAsync(ISyncService syncService,
            IDeviceActionService deviceActionService, IStateService stateService)
        {
            var currentBuild = deviceActionService.GetBuildNumber();
            var lastBuild = await stateService.GetLastBuildAsync();
            if (lastBuild == null || lastBuild != currentBuild)
            {
                // Updated
                var tasks = Task.Run(() => syncService.FullSyncAsync(true));
                await stateService.SetLastBuildAsync(currentBuild);
                return true;
            }
            return false;
        }

        public static async Task SetPreconfiguredSettingsAsync(IDictionary<string, string> configSettings)
        {
            if (configSettings?.Any() != true)
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
                            var urls = new EnvironmentUrlData
                            {
                                Base = settingValue,
                                Api = environmentService.ApiUrl,
                                Identity = environmentService.IdentityUrl,
                                WebVault = environmentService.WebVaultUrl,
                                Icons = environmentService.IconsUrl
                            };
                            await environmentService.SetRegionAsync(urls.Region, urls);
                        }
                        return;
                    default:
                        break;
                }
            }
        }

        public static bool SetAlternateMainPage(AppOptions appOptions)
        {
            if (appOptions != null)
            {
                // this is called after login in or unlocking so we can assume the vault has been unlocked in this transaction here.
                appOptions.HasUnlockedInThisTransaction = true;
                
#if ANDROID
                var fido2MakeCredentialConfirmationUserInterface = ServiceContainer.Resolve<IFido2MakeCredentialConfirmationUserInterface>();
                fido2MakeCredentialConfirmationUserInterface.SetCheckHasVaultBeenUnlockedInThisTransaction(() => appOptions?.HasUnlockedInThisTransaction == true);
#endif

                if (appOptions.FromAutofillFramework && appOptions.SaveType.HasValue)
                {
                    App.MainPage = new NavigationPage(new CipherAddEditPage(appOptions: appOptions));
                    return true;
                }

#if ANDROID
                // If we are waiting for an unlock vault we don't want to trigger 'ExecuteFido2CredentialActionAsync' again,
                // as it's already running. We just need to 'ConfirmUnlockVault' on the 'userVerificationMediatorService'.
                if (fido2MakeCredentialConfirmationUserInterface.IsWaitingUnlockVault)
                {
                    fido2MakeCredentialConfirmationUserInterface.ConfirmVaultUnlocked();
                    return true;
                }
#endif

                if (appOptions.FromFido2Framework && !string.IsNullOrWhiteSpace(appOptions.Fido2CredentialAction))
                {
                    var deviceActionService = Bit.Core.Utilities.ServiceContainer.Resolve<IDeviceActionService>();
                    deviceActionService.ExecuteFido2CredentialActionAsync(appOptions).FireAndForget();
                    return true;
                }

                if (appOptions.Uri != null
                    ||
                    appOptions.OtpData != null)
                {
                    App.MainPage = new NavigationPage(new CipherSelectionPage(appOptions));
                    return true;
                }
                if (appOptions.CreateSend != null)
                {
                    App.MainPage = new NavigationPage(new SendAddEditPage(appOptions));
                    return true;
                }
            }
            return false;
        }

        public static async Task<PreviousPageInfo> ClearPreviousPage()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var previousPage = await stateService.GetPreviousPageInfoAsync();
            if (previousPage != null)
            {
                await stateService.SetPreviousPageInfoAsync(null);
            }
            return previousPage;
        }

        public static async Task<int> IncrementInvalidUnlockAttemptsAsync()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var invalidUnlockAttempts = await stateService.GetInvalidUnlockAttemptsAsync();
            invalidUnlockAttempts++;
            await stateService.SetInvalidUnlockAttemptsAsync(invalidUnlockAttempts);
            return invalidUnlockAttempts;
        }

        public static async Task ResetInvalidUnlockAttemptsAsync()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            await stateService.SetInvalidUnlockAttemptsAsync(null);
        }

        public static async Task<bool> IsVaultTimeoutImmediateAsync()
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var vaultTimeoutMinutes = await stateService.GetVaultTimeoutAsync();
            if (vaultTimeoutMinutes.GetValueOrDefault(-1) == 0)
            {
                return true;
            }
            return false;
        }

        public static string EncodeDataParameter(object obj)
        {
            string EncodeMultibyte(Match match)
            {
                return Convert.ToChar(Convert.ToUInt32($"0x{match.Groups[1].Value}", 16)).ToString();
            }

            var escaped = Uri.EscapeDataString(JsonConvert.SerializeObject(obj));
            var multiByteEscaped = Regex.Replace(escaped, "%([0-9A-F]{2})", EncodeMultibyte);
            return WebUtility.UrlEncode(Convert.ToBase64String(Encoding.UTF8.GetBytes(multiByteEscaped)));
        }

        public static async Task LogOutAsync(string userId, bool userInitiated = false)
        {
            var stateService = ServiceContainer.Resolve<IStateService>("stateService");
            var vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");

            var isActiveAccount = await stateService.IsActiveAccountAsync(userId);

            var isAccountRemoval = await vaultTimeoutService.IsLoggedOutByTimeoutAsync(userId) ||
                                   await vaultTimeoutService.ShouldLogOutByTimeoutAsync(userId);

            if (userId == null)
            {
                userId = await stateService.GetActiveUserIdAsync();
            }

            await stateService.LogoutAccountAsync(userId, userInitiated);

            if (isActiveAccount)
            {
                await ClearServiceCacheAsync();
            }

            if (!userInitiated)
            {
                return;
            }

            // check if we switched active accounts automatically
            if (isActiveAccount && await stateService.GetActiveUserIdAsync() != null)
            {
                var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
                messagingService.Send("switchedAccount");

                var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
                platformUtilsService.ShowToast("info", null, AppResources.AccountSwitchedAutomatically);
                return;
            }

            // check if we logged out a non-active account
            if (!isActiveAccount)
            {
                var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
                if (isAccountRemoval)
                {
                    platformUtilsService.ShowToast("info", null, AppResources.AccountRemovedSuccessfully);
                    return;
                }
                platformUtilsService.ShowToast("info", null, AppResources.AccountLoggedOutSuccessfully);
            }
        }

        public static async Task OnAccountSwitchAsync()
        {
            var environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            await environmentService.SetUrlsFromStorageAsync();
            await ClearServiceCacheAsync();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            await deviceActionService.OnAccountSwitchCompleteAsync();
        }

        public static async Task ClearServiceCacheAsync()
        {
            var tokenService = ServiceContainer.Resolve<ITokenService>("tokenService");
            var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            var settingsService = ServiceContainer.Resolve<ISettingsService>("settingsService");
            var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            var folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            var collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            var sendService = ServiceContainer.Resolve<ISendService>("sendService");
            var passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");
            var fileService = ServiceContainer.Resolve<IFileService>();
            var policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            var searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            var usernameGenerationService = ServiceContainer.Resolve<IUsernameGenerationService>(
                "usernameGenerationService");

            await Task.WhenAll(
                cipherService.ClearCacheAsync(),
                fileService.ClearCacheAsync());
            tokenService.ClearCache();
            cryptoService.ClearCache();
            settingsService.ClearCache();
            folderService.ClearCache();
            collectionService.ClearCache();
            sendService.ClearCache();
            passwordGenerationService.ClearCache();
            policyService.ClearCache();
            searchService.ClearIndex();
            usernameGenerationService.ClearCache();
        }
    }
}
