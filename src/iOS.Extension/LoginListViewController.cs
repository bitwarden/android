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

namespace Bit.iOS.Extension
{
    public partial class LoginListViewController : ExtendedUITableViewController
    {
        public LoginListViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public LoadingViewController LoadingController { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();
            NavItem.Title = AppResources.Items;
            if(!CanAutoFill())
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
            if(Context.ProviderType != Constants.UTTypeAppExtensionFillBrowserAction
                && Context.ProviderType != Constants.UTTypeAppExtensionFillWebViewAction
                && Context.ProviderType != UTType.PropertyList)
            {
                return true;
            }

            return Context.Details?.HasPasswordField ?? false;

        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            LoadingController.CompleteRequest(null);
        }

        partial void AddBarButton_Activated(UIBarButtonItem sender)
        {
            PerformSegue("loginAddSegue", this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var addLoginController = navController.TopViewController as LoginAddViewController;
                if(addLoginController != null)
                {
                    addLoginController.Context = Context;
                    addLoginController.LoginListController = this;
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

                if(Items == null || Items.Count() == 0)
                {
                    _controller.PerformSegue("loginAddSegue", this);
                    return;
                }

                var item = Items.ElementAt(indexPath.Row);
                if(item == null)
                {
                    _controller.LoadingController.CompleteRequest(null);
                    return;
                }

                if(_controller.CanAutoFill() && !string.IsNullOrWhiteSpace(item.Password))
                {
                    string totp = null;
                    if(!_settings.GetValueOrDefault(App.Constants.SettingDisableTotpCopy, false))
                    {
                        totp = GetTotp(item);
                    }

                    _controller.LoadingController.CompleteUsernamePasswordRequest(item.Username, item.Password,
                        item.Fields.Value, totp);
                }
                else if(!string.IsNullOrWhiteSpace(item.Username) || !string.IsNullOrWhiteSpace(item.Password) ||
                    !string.IsNullOrWhiteSpace(item.Totp.Value))
                {
                    var sheet = Dialogs.CreateActionSheet(item.Name, _controller);
                    if(!string.IsNullOrWhiteSpace(item.Username))
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

                    if(!string.IsNullOrWhiteSpace(item.Password))
                    {
                        sheet.AddAction(UIAlertAction.Create(AppResources.CopyPassword, UIAlertActionStyle.Default, a =>
                        {
                            UIPasteboard clipboard = UIPasteboard.General;
                            clipboard.String = item.Password;
                            var alert = Dialogs.CreateMessageAlert(AppResources.CopiedPassword);
                            _controller.PresentViewController(alert, true, () =>
                            {
                                _controller.DismissViewController(true, null);
                            });
                        }));
                    }

                    if(!string.IsNullOrWhiteSpace(item.Totp.Value))
                    {
                        sheet.AddAction(UIAlertAction.Create(AppResources.CopyTotp, UIAlertActionStyle.Default, a =>
                        {
                            var totp = GetTotp(item);
                            if(string.IsNullOrWhiteSpace(totp))
                            {
                                return;
                            }

                            UIPasteboard clipboard = UIPasteboard.General;
                            clipboard.String = totp;
                            var alert = Dialogs.CreateMessageAlert(AppResources.CopiedTotp);
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
