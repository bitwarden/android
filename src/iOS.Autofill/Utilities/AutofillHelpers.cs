using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill.Utilities
{
    public static class AutofillHelpers
    {
        public async static Task TableRowSelectedAsync(UITableView tableView, NSIndexPath indexPath,
            ExtensionTableSource tableSource, CredentialProviderViewController cpViewController,
            UITableViewController controller, IPasswordRepromptService passwordRepromptService,
            string loginAddSegue)
        {
            tableView.DeselectRow(indexPath, true);
            tableView.EndEditing(true);

            if (tableSource.Items == null || tableSource.Items.Count() == 0)
            {
                controller.PerformSegue(loginAddSegue, tableSource);
                return;
            }
            var item = tableSource.Items.ElementAt(indexPath.Row);
            if (item == null)
            {
                cpViewController.CompleteRequest();
                return;
            }

            if (item.Reprompt != Bit.Core.Enums.CipherRepromptType.None && !await passwordRepromptService.ShowPasswordPromptAsync())
            {
                return;
            }

            if (!string.IsNullOrWhiteSpace(item.Username) && !string.IsNullOrWhiteSpace(item.Password))
            {
                string totp = null;
                var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
                var disableTotpCopy = await storageService.GetAsync<bool?>(Bit.Core.Constants.DisableAutoTotpCopyKey);
                if (!disableTotpCopy.GetValueOrDefault(false))
                {
                    var userService = ServiceContainer.Resolve<IUserService>("userService");
                    var canAccessPremiumAsync = await userService.CanAccessPremiumAsync();
                    if (!string.IsNullOrWhiteSpace(item.Totp) &&
                        (canAccessPremiumAsync || item.CipherView.OrganizationUseTotp))
                    {
                        var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                        totp = await totpService.GetCodeAsync(item.Totp);
                    }
                }
                cpViewController.CompleteRequest(item.Id, item.Username, item.Password, totp);
            }
            else if (!string.IsNullOrWhiteSpace(item.Username) || !string.IsNullOrWhiteSpace(item.Password) ||
                !string.IsNullOrWhiteSpace(item.Totp))
            {
                var sheet = Dialogs.CreateActionSheet(item.Name, controller);
                if (!string.IsNullOrWhiteSpace(item.Username))
                {
                    sheet.AddAction(UIAlertAction.Create(AppResources.CopyUsername, UIAlertActionStyle.Default, a =>
                    {
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = item.Username;
                        var alert = Dialogs.CreateMessageAlert(AppResources.CopyUsername);
                        controller.PresentViewController(alert, true, () =>
                        {
                            controller.DismissViewController(true, null);
                        });
                    }));
                }

                if (!string.IsNullOrWhiteSpace(item.Password))
                {
                    sheet.AddAction(UIAlertAction.Create(AppResources.CopyPassword, UIAlertActionStyle.Default, a =>
                    {
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = item.Password;
                        var alert = Dialogs.CreateMessageAlert(
                            string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
                        controller.PresentViewController(alert, true, () =>
                        {
                            controller.DismissViewController(true, null);
                        });
                    }));
                }

                if (!string.IsNullOrWhiteSpace(item.Totp))
                {
                    sheet.AddAction(UIAlertAction.Create(AppResources.CopyTotp, UIAlertActionStyle.Default, async a =>
                    {
                        var totp = await tableSource.GetTotpAsync(item);
                        if (string.IsNullOrWhiteSpace(totp))
                        {
                            return;
                        }
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = totp;
                        var alert = Dialogs.CreateMessageAlert(
                            string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
                        controller.PresentViewController(alert, true, () =>
                        {
                            controller.DismissViewController(true, null);
                        });
                    }));
                }
                sheet.AddAction(UIAlertAction.Create(AppResources.Cancel, UIAlertActionStyle.Cancel, null));
                controller.PresentViewController(sheet, true, null);
            }
            else
            {
                var alert = Dialogs.CreateAlert(null, AppResources.NoUsernamePasswordConfigured, AppResources.Ok);
                controller.PresentViewController(alert, true, null);
            }
        }
    }
}