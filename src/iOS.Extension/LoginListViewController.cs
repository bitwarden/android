using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.iOS.Extension.Models;
using Foundation;
using UIKit;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.iOS.Core.Utilities;
using System.Threading.Tasks;
using Bit.iOS.Core;
using MobileCoreServices;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;
using Bit.App.Models;

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
            NavItem.Title = AppResources.Logins;
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

        public class TableSource : UITableViewSource
        {
            private const string CellIdentifier = "TableCell";

            private IEnumerable<LoginViewModel> _tableItems = new List<LoginViewModel>();
            private Context _context;
            private LoginListViewController _controller;
            private ILoginService _loginService;
            private ISettings _settings;
            private bool _isPremium;

            public TableSource(LoginListViewController controller)
            {
                _context = controller.Context;
                _controller = controller;
                _isPremium = Resolver.Resolve<ITokenService>()?.TokenPremium ?? false;
                _loginService = Resolver.Resolve<ILoginService>();
                _settings = Resolver.Resolve<ISettings>();
            }

            public async Task LoadItemsAsync()
            {
                var combinedLogins = new List<Login>();

                var logins = await _loginService.GetAllAsync(_context.UrlString);
                if(logins?.Item1 != null)
                {
                    combinedLogins.AddRange(logins.Item1);
                }
                if(logins?.Item2 != null)
                {
                    combinedLogins.AddRange(logins.Item2);
                }

                _tableItems = combinedLogins.Select(s => new LoginViewModel(s))
                    .OrderBy(s => s.Name)
                    .ThenBy(s => s.Username)
                    .ToList() ?? new List<LoginViewModel>();
            }

            public IEnumerable<LoginViewModel> TableItems { get; set; }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                return _tableItems == null || _tableItems.Count() == 0 ? 1 : _tableItems.Count();
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if(_tableItems == null || _tableItems.Count() == 0)
                {
                    var noDataCell = new UITableViewCell(UITableViewCellStyle.Default, "NoDataCell");
                    noDataCell.TextLabel.Text = AppResources.NoLoginsTap;
                    noDataCell.TextLabel.TextAlignment = UITextAlignment.Center;
                    noDataCell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                    noDataCell.TextLabel.Lines = 0;
                    return noDataCell;
                }

                var cell = tableView.DequeueReusableCell(CellIdentifier);

                // if there are no cells to reuse, create a new one
                if(cell == null)
                {
                    Debug.WriteLine("BW Log, Make new cell for list.");
                    cell = new UITableViewCell(UITableViewCellStyle.Subtitle, CellIdentifier);
                    cell.DetailTextLabel.TextColor = cell.DetailTextLabel.TintColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);
                }
                return cell;
            }

            public override void WillDisplay(UITableView tableView, UITableViewCell cell, NSIndexPath indexPath)
            {
                if(_tableItems == null || _tableItems.Count() == 0 || cell == null)
                {
                    return;
                }

                var item = _tableItems.ElementAt(indexPath.Row);
                cell.TextLabel.Text = item.Name;
                cell.DetailTextLabel.Text = item.Username;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);

                if(_tableItems == null || _tableItems.Count() == 0)
                {
                    _controller.PerformSegue("loginAddSegue", this);
                    return;
                }

                var item = _tableItems.ElementAt(indexPath.Row);
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

                    _controller.LoadingController.CompleteUsernamePasswordRequest(item.Username, item.Password, totp);
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

            private string GetTotp(LoginViewModel item)
            {
                string totp = null;
                if(_isPremium)
                {
                    if(item != null && !string.IsNullOrWhiteSpace(item.Totp.Value))
                    {
                        totp = App.Utilities.Crypto.Totp(item.Totp.Value);
                    }
                }

                return totp;
            }
        }
    }
}
