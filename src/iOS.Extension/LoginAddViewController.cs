using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.iOS.Core.Views;
using Bit.iOS.Extension.Models;
using Foundation;
using UIKit;
using XLabs.Ioc;
using Bit.App;
using Plugin.Connectivity.Abstractions;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Controllers;

namespace Bit.iOS.Extension
{
    public partial class LoginAddViewController : ExtendedUITableViewController
    {
        private ILoginService _loginService;
        private IFolderService _folderService;
        private IConnectivity _connectivity;
        private IEnumerable<Folder> _folders;
        private IGoogleAnalyticsService _googleAnalyticsService;

        public LoginAddViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public LoginListViewController LoginListController { get; set; }
        public LoadingViewController LoadingController { get; set; }
        public FormEntryTableViewCell NameCell { get; set; } = new FormEntryTableViewCell(AppResources.Name);
        public FormEntryTableViewCell UriCell { get; set; } = new FormEntryTableViewCell(AppResources.URI);
        public FormEntryTableViewCell UsernameCell { get; set; } = new FormEntryTableViewCell(AppResources.Username);
        public FormEntryTableViewCell PasswordCell { get; set; } = new FormEntryTableViewCell(AppResources.Password);
        public UITableViewCell GeneratePasswordCell { get; set; } = new UITableViewCell(UITableViewCellStyle.Subtitle, "GeneratePasswordCell");
        public SwitchTableViewCell FavoriteCell { get; set; } = new SwitchTableViewCell(AppResources.Favorite);
        public FormEntryTableViewCell NotesCell { get; set; } = new FormEntryTableViewCell(useTextView: true, height: 90);
        public PickerTableViewCell FolderCell { get; set; } = new PickerTableViewCell(AppResources.Folder);

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            _loginService = Resolver.Resolve<ILoginService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _folderService = Resolver.Resolve<IFolderService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            NavItem.Title = AppResources.AddLogin;
            CancelBarButton.Title = AppResources.Cancel;
            SaveBarButton.Title = AppResources.Save;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            NameCell.TextField.Text = Context?.Uri?.Host ?? string.Empty;
            NameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            NameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UriCell.TextField.BecomeFirstResponder();
                return true;
            };

            UriCell.TextField.Text = Context?.UrlString ?? string.Empty;
            UriCell.TextField.KeyboardType = UIKeyboardType.Url;
            UriCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UriCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UsernameCell.TextField.BecomeFirstResponder();
                return true;
            };

            UsernameCell.TextField.AutocapitalizationType = UITextAutocapitalizationType.None;
            UsernameCell.TextField.AutocorrectionType = UITextAutocorrectionType.No;
            UsernameCell.TextField.SpellCheckingType = UITextSpellCheckingType.No;
            UsernameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UsernameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                PasswordCell.TextField.BecomeFirstResponder();
                return true;
            };

            PasswordCell.TextField.SecureTextEntry = true;
            PasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            PasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                NotesCell.TextView.BecomeFirstResponder();
                return true;
            };

            GeneratePasswordCell.TextLabel.Text = AppResources.GeneratePassword;
            GeneratePasswordCell.Accessory = UITableViewCellAccessory.DisclosureIndicator;

            _folders = _folderService.GetAllAsync().GetAwaiter().GetResult();
            var folderNames = _folders.Select(s => s.Name.Decrypt()).OrderBy(s => s).ToList();
            folderNames.Insert(0, AppResources.FolderNone);
            FolderCell.Items = folderNames;

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 70;
            TableView.Source = new TableSource(this);
            TableView.AllowsSelection = true;

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            if(LoginListController != null)
            {
                DismissViewController(true, null);
            }
            else
            {
                LoadingController.CompleteRequest(null);
            }
        }

        async partial void SaveBarButton_Activated(UIBarButtonItem sender)
        {
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
                return;
            }

            if(string.IsNullOrWhiteSpace(PasswordCell.TextField.Text))
            {
                DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Password), AppResources.Ok);
                return;
            }

            if(string.IsNullOrWhiteSpace(NameCell.TextField.Text))
            {
                DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Name), AppResources.Ok);
                return;
            }

            var login = new Login
            {
                Uri = string.IsNullOrWhiteSpace(UriCell.TextField.Text) ? null : UriCell.TextField.Text.Encrypt(),
                Name = string.IsNullOrWhiteSpace(NameCell.TextField.Text) ? null : NameCell.TextField.Text.Encrypt(),
                Username = string.IsNullOrWhiteSpace(UsernameCell.TextField.Text) ? null : UsernameCell.TextField.Text.Encrypt(),
                Password = string.IsNullOrWhiteSpace(PasswordCell.TextField.Text) ? null : PasswordCell.TextField.Text.Encrypt(),
                Notes = string.IsNullOrWhiteSpace(NotesCell.TextView.Text) ? null : NotesCell.TextView.Text.Encrypt(),
                Favorite = FavoriteCell.Switch.On,
                FolderId = FolderCell.SelectedIndex == 0 ? null : _folders.ElementAtOrDefault(FolderCell.SelectedIndex - 1)?.Id
            };

            var saveTask = _loginService.SaveAsync(login);
            var loadingAlert = Dialogs.CreateLoadingAlert(AppResources.Saving);
            PresentViewController(loadingAlert, true, null);
            await saveTask;

            if(saveTask.Result.Succeeded)
            {
                _googleAnalyticsService.TrackExtensionEvent("CreatedLogin");
                if(LoginListController != null)
                {
                    LoginListController.DismissModal();
                }
                else if(LoadingController != null)
                {
                    LoadingController.CompleteUsernamePasswordRequest(UsernameCell.TextField.Text, PasswordCell.TextField.Text,
                        null);
                }
            }
            else if(saveTask.Result.Errors.Count() > 0)
            {
                DisplayAlert(AppResources.AnErrorHasOccurred, saveTask.Result.Errors.First().Message, AppResources.Ok);
            }
            else
            {
                DisplayAlert(null, AppResources.AnErrorHasOccurred, AppResources.Ok);
            }
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var passwordGeneratorController = navController.TopViewController as PasswordGeneratorViewController;
                if(passwordGeneratorController != null)
                {
                    passwordGeneratorController.Context = Context;
                    passwordGeneratorController.Parent = this;
                }
            }
        }

        public void DisplayAlert(string title, string message, string accept)
        {
            var alert = Dialogs.CreateAlert(title, message, accept);
            PresentViewController(alert, true, null);
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
        }

        public class TableSource : UITableViewSource
        {
            private LoginAddViewController _controller;

            public TableSource(LoginAddViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    if(indexPath.Row == 0)
                    {
                        return _controller.NameCell;
                    }
                    else if(indexPath.Row == 1)
                    {
                        return _controller.UriCell;
                    }
                    else if(indexPath.Row == 2)
                    {
                        return _controller.UsernameCell;
                    }
                    else if(indexPath.Row == 3)
                    {
                        return _controller.PasswordCell;
                    }
                    else if(indexPath.Row == 4)
                    {
                        return _controller.GeneratePasswordCell;
                    }
                }
                else if(indexPath.Section == 1)
                {
                    if(indexPath.Row == 0)
                    {
                        return _controller.FolderCell;
                    }
                    else if(indexPath.Row == 1)
                    {
                        return _controller.FavoriteCell;
                    }
                }
                else if(indexPath.Section == 2)
                {
                    return _controller.NotesCell;
                }

                return new UITableViewCell();
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                return 3;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if(section == 0)
                {
                    return 5;
                }
                else if(section == 1)
                {
                    return 2;
                }
                else
                {
                    return 1;
                }
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                return UITableView.AutomaticDimension;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                if(section == 0)
                {
                    return AppResources.LoginInformation;
                }
                else if(section == 2)
                {
                    return AppResources.Notes;
                }

                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);

                if(indexPath.Section == 0 && indexPath.Row == 4)
                {
                    _controller.PerformSegue("passwordGeneratorSegue", this);
                }

                var cell = tableView.CellAt(indexPath);
                if(cell == null)
                {
                    return;
                }

                var selectableCell = cell as ISelectable;
                if(selectableCell != null)
                {
                    selectableCell.Select();
                }
            }
        }
    }
}
