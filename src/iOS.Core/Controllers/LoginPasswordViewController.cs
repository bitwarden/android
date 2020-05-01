using System;
using UIKit;
using Foundation;
using Bit.iOS.Core.Views;
using Bit.App.Resources;
using Bit.iOS.Core.Utilities;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LoginPasswordViewController : ExtendedUITableViewController
    {
        private IDeviceActionService _deviceActionService;
        private IStorageService _storageService;
        private IAuthService _authService;
        private ISyncService _syncService;

        public LoginPasswordViewController(IntPtr handle)
            : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSubmitButton { get; }
        public abstract Action Success { get; }
        public abstract Action Cancel { get; }

        public FormEntryTableViewCell EmailCell { get; set; } = new FormEntryTableViewCell(
            AppResources.EmailAddress);
        public FormEntryTableViewCell MasterPasswordCell { get; set; } = new FormEntryTableViewCell(
            AppResources.MasterPassword);

        public override void ViewDidLoad()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            BaseNavItem.Title = AppResources.Bitwarden;
            BaseCancelButton.Title = AppResources.Cancel;
            BaseSubmitButton.Title = AppResources.LogIn;

            var descriptor = UIFontDescriptor.PreferredBody;
            
            MasterPasswordCell.TextField.SecureTextEntry = true;
            MasterPasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Go;
            MasterPasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                // TODO This isn't working - bricks application
                LogInAsync().GetAwaiter().GetResult();
                return true;
            };

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 70; // Should this be adjusted for two cells?
            TableView.Source = new TableSource(this);
            TableView.AllowsSelection = true;
            
            // Attempt to programatically set the user's email
            EmailCell.TextField.Text = _storageService.GetAsync<string>("rememberedEmail").GetAwaiter().GetResult();

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            if (string.IsNullOrWhiteSpace(EmailCell.TextField.Text))
            {
                EmailCell.TextField.BecomeFirstResponder();
            }
            else
            {
                MasterPasswordCell.TextField.BecomeFirstResponder();
            }
        }

        protected async Task LogInAsync()
        { 
            var email = EmailCell.TextField.Text;
            var password = MasterPasswordCell.TextField.Text;
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                var connectivityAlert = Dialogs.CreateAlert(AppResources.InternetConnectionRequiredTitle,
                    AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
                PresentViewController(connectivityAlert, true, null);
                return;
            }
            if (string.IsNullOrWhiteSpace(email))
            {
                var emailEmptyAlert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.Ok, (a) => EmailCell.TextField.BecomeFirstResponder());
                PresentViewController(emailEmptyAlert, true, null);
                return;
            }
            if (!email.Contains("@"))
            {
                var emailInvalidAlert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    AppResources.InvalidEmail, AppResources.Ok,(a) => EmailCell.TextField.BecomeFirstResponder());
                PresentViewController(emailInvalidAlert, true, null);
                return;
            }
            if (string.IsNullOrWhiteSpace(password))
            {
                var passwordEmptyAlert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.Ok,(a) => MasterPasswordCell.TextField.BecomeFirstResponder());
                PresentViewController(passwordEmptyAlert, true, null);
                return;
            }
            
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                var response = await _authService.LogInAsync(email, password);
                await _deviceActionService.HideLoadingAsync();
                if (response.TwoFactor)
                {
                    // TODO Segue to Two Factor page
                }
                else
                {
                    var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
                    EmailCell.TextField.ResignFirstResponder();
                    MasterPasswordCell.TextField.ResignFirstResponder();
                    Success();
                }
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    var errorAlert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                        e.Error.GetSingleMessage(), AppResources.Ok);
                    PresentViewController(errorAlert, true, null);
                }
            }
        }

        public class TableSource : ExtendedUITableViewSource
        {
            private LoginPasswordViewController _controller;

            public TableSource(LoginPasswordViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if (indexPath.Section == 0)
                {
                    if (indexPath.Row == 0)
                    {
                        return _controller.EmailCell;
                    } 
                    if (indexPath.Row == 1)
                    {
                        return _controller.MasterPasswordCell;
                    }
                }
                
                return new ExtendedUITableViewCell();
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                return 1;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                return 2; // EmailCell and MasterPasswordCell
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                return UITableView.AutomaticDimension;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
                var cell = tableView.CellAt(indexPath);
                if (cell == null)
                {
                    return;
                }
                if (cell is ISelectable selectableCell)
                {
                    selectableCell.Select();
                }
            }
        }
    }
}
