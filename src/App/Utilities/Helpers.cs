using Bit.App.Abstractions;
using Bit.App.Enums;
using Bit.App.Models.Page;
using Bit.App.Pages;
using Bit.App.Resources;
using Plugin.Settings.Abstractions;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Utilities
{
    public static class Helpers
    {
        public static readonly DateTime Epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long EpocUtcNow()
        {
            return (long)(DateTime.UtcNow - Epoc).TotalMilliseconds;
        }

        public static T OnPlatform<T>(T iOS = default(T), T Android = default(T),
            T WinPhone = default(T), T Windows = default(T), string platform = null)
        {
            if(platform == null)
            {
                platform = Device.RuntimePlatform;
            }

            switch(platform)
            {
                case Device.iOS:
                    return iOS;
                case Device.Android:
                    return Android;
                case Device.WinPhone:
                    return WinPhone;
                case Device.UWP:
                    return Windows;
                default:
                    throw new Exception("Unsupported platform.");
            }
        }

        public static bool InDebugMode()
        {
#if DEBUG
            return true;
#else
            return false;
#endif
        }

        public static bool PerformUpdateTasks(ISettings settings,
            IAppInfoService appInfoService, IDatabaseService databaseService, ISyncService syncService)
        {
            var lastBuild = settings.GetValueOrDefault(Constants.LastBuildKey, null);
            if(InDebugMode() || lastBuild == null || lastBuild != appInfoService.Build)
            {
                settings.AddOrUpdateValue(Constants.LastBuildKey, appInfoService.Build);
                databaseService.CreateTables();
                var task = Task.Run(async () => await syncService.FullSyncAsync(true));
                return true;
            }

            return false;
        }

        public static string GetEmptyTableSectionTitle()
        {
            if(Device.RuntimePlatform == Device.iOS)
            {
                return string.Empty;
            }

            return " ";
        }

        public static string ToolbarImage(string image)
        {
            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Android)
            {
                return null;
            }

            return image;
        }

        public static async void CipherMoreClickedAsync(Page page, VaultListPageModel.Cipher cipher, bool autofill)
        {
            var buttons = new List<string> { AppResources.View, AppResources.Edit };

            if(cipher.Type == CipherType.Login)
            {
                if(!string.IsNullOrWhiteSpace(cipher.LoginPassword.Value))
                {
                    buttons.Add(AppResources.CopyPassword);
                }
                if(!string.IsNullOrWhiteSpace(cipher.LoginUsername))
                {
                    buttons.Add(AppResources.CopyUsername);
                }
                if(!autofill && !string.IsNullOrWhiteSpace(cipher.LoginUri) && (cipher.LoginUri.StartsWith("http://")
                    || cipher.LoginUri.StartsWith("https://")))
                {
                    buttons.Add(AppResources.GoToWebsite);
                }
            }
            else if(cipher.Type == CipherType.Card)
            {
                if(!string.IsNullOrWhiteSpace(cipher.CardNumber))
                {
                    buttons.Add(AppResources.CopyNumber);
                }
                if(!string.IsNullOrWhiteSpace(cipher.CardCode.Value))
                {
                    buttons.Add(AppResources.CopySecurityCode);
                }
            }

            var selection = await page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, buttons.ToArray());

            if(selection == AppResources.View)
            {
                var p = new VaultViewCipherPage(cipher.Type, cipher.Id);
                await page.Navigation.PushForDeviceAsync(p);
            }
            else if(selection == AppResources.Edit)
            {
                var p = new VaultEditCipherPage(cipher.Id);
                await page.Navigation.PushForDeviceAsync(p);
            }
            else if(selection == AppResources.CopyPassword)
            {
                CipherCopy(cipher.LoginPassword.Value, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                CipherCopy(cipher.LoginUsername, AppResources.Username);
            }
            else if(selection == AppResources.GoToWebsite)
            {
                Device.OpenUri(new Uri(cipher.LoginUri));
            }
            else if(selection == AppResources.CopyNumber)
            {
                CipherCopy(cipher.CardNumber, AppResources.Number);
            }
            else if(selection == AppResources.CopySecurityCode)
            {
                CipherCopy(cipher.CardCode.Value, AppResources.SecurityCode);
            }
        }

        public static void CipherCopy(string copyText, string alertLabel)
        {
            var daService = Resolver.Resolve<IDeviceActionService>();
            daService.CopyToClipboard(copyText);
            daService.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        public static async void AddCipher(Page page, string folderId)
        {
            var type = await page.DisplayActionSheet(AppResources.SelectTypeAdd, AppResources.Cancel, null,
                AppResources.TypeLogin, AppResources.TypeCard, AppResources.TypeIdentity, AppResources.TypeSecureNote);

            var selectedType = CipherType.SecureNote;
            if(type == AppResources.Cancel)
            {
                return;
            }
            else if(type == AppResources.TypeLogin)
            {
                selectedType = CipherType.Login;
            }
            else if(type == AppResources.TypeCard)
            {
                selectedType = CipherType.Card;
            }
            else if(type == AppResources.TypeIdentity)
            {
                selectedType = CipherType.Identity;
            }

            var addPage = new VaultAddCipherPage(selectedType, defaultFolderId: folderId);
            await page.Navigation.PushForDeviceAsync(addPage);
        }
    }
}
