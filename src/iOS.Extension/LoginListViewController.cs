using System;
using System.Linq;
using Bit.iOS.Extension.Models;
using Foundation;
using UIKit;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core;
using MobileCoreServices;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.iOS.Core.Views;
using Bit.Core.Utilities;
using Bit.Core.Abstractions;

namespace Bit.iOS.Extension
{
    public partial class LoginListViewController : ExtendedUITableViewController
    {
        public LoginListViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();
            AddBarButton.TintColor = ThemeHelpers.NavBarTextColor;
            CancelBarButton.TintColor = ThemeHelpers.NavBarTextColor;
            NavItem.Title = AppResources.Items;
            if (!CanAutoFill())
            {
                CancelBarButton.Title = AppResources.Close;
            }
            else
            {
                CancelBarButton.Title = AppResources.Cancel;
            }
            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 44;
            TableView.Source = new TableSource(this);
            await ((TableSource)TableView.Source).LoadItemsAsync();
        }

        public bool CanAutoFill()
        {
            if (Context.ProviderType != Constants.UTTypeAppExtensionFillBrowserAction
                && Context.ProviderType != Constants.UTTypeAppExtensionFillWebViewAction
                && Context.ProviderType != UTType.PropertyList
                && Context.ProviderType != Constants.UTTypeAppExtensionUrl)
            {
                return true;
            }
            return Context.Details?.HasPasswordField ?? false;

        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
        
        private void Cancel()
        {
            LoadingController.CompleteRequest(null, null);
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue("loginAddSegue", this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginAddViewController addLoginController)
                {
                    addLoginController.Context = Context;
                    addLoginController.LoginListController = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(addLoginController.DismissModalAction);
                }
            }
        }

        public void DismissModal()
        {
            DismissViewController(true, async () =>
            {
                await ((TableSource)TableView.Source).LoadItemsAsync();
                TableView.ReloadData();
            });
        }

        public class TableSource : ExtensionTableSource
        {
            private LoginListViewController _controller;

            public TableSource(LoginListViewController controller)
                : base(controller.Context, controller)
            {
                _controller = controller;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);

                if (Items == null || Items.Count() == 0)
                {
                    _controller.PerformSegue("loginAddSegue", this);
                    return;
                }

                var item = Items.ElementAt(indexPath.Row);
                if (item == null)
                {
                    _controller.LoadingController.CompleteRequest(null, null);
                    return;
                }

                if (_controller.CanAutoFill() && !string.IsNullOrWhiteSpace(item.Password))
                {
                    string totp = null;
                    var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
                    var disableTotpCopy = storageService.GetAsync<bool?>(
                        Bit.Core.Constants.DisableAutoTotpCopyKey).GetAwaiter().GetResult();
                    if (!disableTotpCopy.GetValueOrDefault(false))
                    {
                        totp = GetTotpAsync(item).GetAwaiter().GetResult();
                    }
                    _controller.LoadingController.CompleteUsernamePasswordRequest(
                        item.Id, item.Username, item.Password, item.Fields, totp);
                }
                else if (!string.IsNullOrWhiteSpace(item.Username) || !string.IsNullOrWhiteSpace(item.Password) ||
                    !string.IsNullOrWhiteSpace(item.Totp))
                {
                    var sheet = Dialogs.CreateActionSheet(item.Name, _controller);
                    if (!string.IsNullOrWhiteSpace(item.Username))
                    {
                        sheet.AddAction(UIAlertAction.Create(AppResources.CopyUsername, UIAlertActionStyle.Default, a =>
                        {
                            UIPasteboard clipboard = UIPasteboard.General;
                            clipboard.String = item.Username;
                            var alert = Dialogs.CreateMessageAlert(AppResources.CopyUsername);
                            _controller.PresentViewController(alert, true, () =>
                            {
                                _controller.DismissViewController(true, null);
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
                            _controller.PresentViewController(alert, true, () =>
                            {
                                _controller.DismissViewController(true, null);
                            });
                        }));
                    }
                    if (!string.IsNullOrWhiteSpace(item.Totp))
                    {
                        sheet.AddAction(UIAlertAction.Create(AppResources.CopyTotp, UIAlertActionStyle.Default,
                            async a =>
                            {
                                var totp = await GetTotpAsync(item);
                                if (string.IsNullOrWhiteSpace(totp))
                                {
                                    return;
                                }
                                UIPasteboard clipboard = UIPasteboard.General;
                                clipboard.String = totp;
                                var alert = Dialogs.CreateMessageAlert(
                                    string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
                                _controller.PresentViewController(alert, true, () =>
                                {
                                    _controller.DismissViewController(true, null);
                                });
                            }));
                    }
                    sheet.AddAction(UIAlertAction.Create(AppResources.Cancel, UIAlertActionStyle.Cancel, null));
                    _controller.PresentViewController(sheet, true, null);
                }
                else
                {
                    var alert = Dialogs.CreateAlert(null, AppResources.NoUsernamePasswordConfigured, AppResources.Ok);
                    _controller.PresentViewController(alert, true, null);
                }
            }
        }
    }
}
