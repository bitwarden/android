using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public static class AppHelpers
    {
        public static async Task<string> CipherListOptions(ContentPage page, CipherView cipher)
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            var options = new List<string> { AppResources.View, AppResources.Edit };
            if(cipher.Type == Core.Enums.CipherType.Login)
            {
                if(!string.IsNullOrWhiteSpace(cipher.Login.Username))
                {
                    options.Add(AppResources.CopyUsername);
                }
                if(!string.IsNullOrWhiteSpace(cipher.Login.Password))
                {
                    options.Add(AppResources.CopyPassword);
                }
                if(!string.IsNullOrWhiteSpace(cipher.Login.Totp))
                {
                    var userService = ServiceContainer.Resolve<IUserService>("userService");
                    var canAccessPremium = await userService.CanAccessPremiumAsync();
                    if(canAccessPremium || cipher.OrganizationUseTotp)
                    {
                        options.Add(AppResources.CopyTotp);
                    }
                }
                if(cipher.Login.CanLaunch)
                {
                    options.Add(AppResources.Launch);
                }
            }
            else if(cipher.Type == Core.Enums.CipherType.Card)
            {
                if(!string.IsNullOrWhiteSpace(cipher.Card.Number))
                {
                    options.Add(AppResources.CopyNumber);
                }
                if(!string.IsNullOrWhiteSpace(cipher.Card.Code))
                {
                    options.Add(AppResources.CopySecurityCode);
                }
            }
            else if(cipher.Type == Core.Enums.CipherType.SecureNote)
            {
                if(!string.IsNullOrWhiteSpace(cipher.Notes))
                {
                    options.Add(AppResources.CopyNotes);
                }
            }
            var selection = await page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, options.ToArray());
            if(selection == AppResources.View)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new ViewPage(cipher.Id)));
            }
            else if(selection == AppResources.Edit)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new AddEditPage(cipher.Id)));
            }
            else if(selection == AppResources.CopyUsername)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Login.Username);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Username));
            }
            else if(selection == AppResources.CopyPassword)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Login.Password);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
            }
            else if(selection == AppResources.CopyTotp)
            {
                var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                var totp = await totpService.GetCodeAsync(cipher.Login.Totp);
                if(!string.IsNullOrWhiteSpace(totp))
                {
                    await platformUtilsService.CopyToClipboardAsync(totp);
                    platformUtilsService.ShowToast("info", null,
                        string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
                }
            }
            else if(selection == AppResources.Launch)
            {
                platformUtilsService.LaunchUri(cipher.Login.LaunchUri);
            }
            else if(selection == AppResources.CopyNumber)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Card.Number);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.Number));
            }
            else if(selection == AppResources.CopySecurityCode)
            {
                await platformUtilsService.CopyToClipboardAsync(cipher.Card.Code);
                platformUtilsService.ShowToast("info", null,
                    string.Format(AppResources.ValueHasBeenCopied, AppResources.SecurityCode));
            }
            else if(selection == AppResources.CopyNotes)
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
            if(!Migration.MigrationHelpers.NeedsMigration())
            {
                if(lastBuild == null)
                {
                    // Installed
                    var currentLock = await storageService.GetAsync<int?>(Constants.LockOptionKey);
                    if(currentLock == null)
                    {
                        await storageService.SaveAsync(Constants.LockOptionKey, 15);
                    }
                }
                else if(lastBuild != currentBuild)
                {
                    // Updated
                    var tasks = Task.Run(() => syncService.FullSyncAsync(true));
                }
            }
            if(lastBuild != currentBuild)
            {
                await storageService.SaveAsync(Constants.LastBuildKey, currentBuild);
                return true;
            }
            return false;
        }
    }
}
