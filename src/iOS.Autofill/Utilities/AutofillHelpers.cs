using System;
using System.Linq;
using Bit.App.Resources;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill.Utilities
{
    public static class AutofillHelpers
    {
        /*
        public static void TableRowSelected(UITableView tableView, NSIndexPath indexPath,
            ExtensionTableSource tableSource, CredentialProviderViewController cpViewController,
            UITableViewController controller, ISettings settings, string loginAddSegue)
        {
            tableView.DeselectRow(indexPath, true);
            tableView.EndEditing(true);

            if(tableSource.Items == null || tableSource.Items.Count() == 0)
            {
                controller.PerformSegue(loginAddSegue, tableSource);
                return;
            }

            var item = tableSource.Items.ElementAt(indexPath.Row);
            if(item == null)
            {
                cpViewController.CompleteRequest(null);
                return;
            }

            if(!string.IsNullOrWhiteSpace(item.Username) && !string.IsNullOrWhiteSpace(item.Password))
            {
                string totp = null;
                if(!settings.GetValueOrDefault(App.Constants.SettingDisableTotpCopy, false))
                {
                    totp = tableSource.GetTotp(item);
                }

                cpViewController.CompleteRequest(item.Username, item.Password, totp);
            }
            else if(!string.IsNullOrWhiteSpace(item.Username) || !string.IsNullOrWhiteSpace(item.Password) ||
                !string.IsNullOrWhiteSpace(item.Totp.Value))
            {
                var sheet = Dialogs.CreateActionSheet(item.Name, controller);
                if(!string.IsNullOrWhiteSpace(item.Username))
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

                if(!string.IsNullOrWhiteSpace(item.Password))
                {
                    sheet.AddAction(UIAlertAction.Create(AppResources.CopyPassword, UIAlertActionStyle.Default, a =>
                    {
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = item.Password;
                        var alert = Dialogs.CreateMessageAlert(AppResources.CopiedPassword);
                        controller.PresentViewController(alert, true, () =>
                        {
                            controller.DismissViewController(true, null);
                        });
                    }));
                }

                if(!string.IsNullOrWhiteSpace(item.Totp.Value))
                {
                    sheet.AddAction(UIAlertAction.Create(AppResources.CopyTotp, UIAlertActionStyle.Default, a =>
                    {
                        var totp = tableSource.GetTotp(item);
                        if(string.IsNullOrWhiteSpace(totp))
                        {
                            return;
                        }

                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = totp;
                        var alert = Dialogs.CreateMessageAlert(AppResources.CopiedTotp);
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
        */
    }
}