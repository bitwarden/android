using System;
using UIKit;
using Foundation;
using Bit.iOS.Core.Views;
using Bit.App.Resources;
using Bit.iOS.Core.Utilities;
using Bit.App.Abstractions;
using System.Linq;
using Bit.iOS.Core.Controllers;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LockPasswordViewController : ExtendedUITableViewController
    {
        //private IAuthService _authService;
        //private ICryptoService _cryptoService;

        public LockPasswordViewController(IntPtr handle) : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSubmitButton { get; }
        public abstract Action Success { get; }

        public FormEntryTableViewCell MasterPasswordCell { get; set; } = new FormEntryTableViewCell(
            AppResources.MasterPassword, useLabelAsPlaceholder: true);

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            // _authService = Resolver.Resolve<IAuthService>();
            // _cryptoService = Resolver.Resolve<ICryptoService>();

            BaseNavItem.Title = AppResources.VerifyMasterPassword;
            BaseCancelButton.Title = AppResources.Cancel;
            BaseSubmitButton.Title = AppResources.Submit;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            var descriptor = UIFontDescriptor.PreferredBody;

            MasterPasswordCell.TextField.SecureTextEntry = true;
            MasterPasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Go;
            MasterPasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                // CheckPassword();
                return true;
            };

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 70;
            TableView.Source = new TableSource(this);
            TableView.AllowsSelection = true;

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            MasterPasswordCell.TextField.BecomeFirstResponder();
        }

        /*
        protected void CheckPassword()
        {
            if(string.IsNullOrWhiteSpace(MasterPasswordCell.TextField.Text))
            {
                var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword), AppResources.Ok);
                PresentViewController(alert, true, null);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(MasterPasswordCell.TextField.Text, _authService.Email,
                 _authService.Kdf, _authService.KdfIterations);
            if(key.Key.SequenceEqual(_cryptoService.Key.Key))
            {
                _appSettingsService.Locked = false;
                MasterPasswordCell.TextField.ResignFirstResponder();
                Success();
            }
            else
            {
                // TODO: keep track of invalid attempts and logout?

                var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    string.Format(null, AppResources.InvalidMasterPassword), AppResources.Ok, (a) =>
                    {

                        MasterPasswordCell.TextField.Text = string.Empty;
                        MasterPasswordCell.TextField.BecomeFirstResponder();
                    });

                PresentViewController(alert, true, null);
            }
        }
        */

        public class TableSource : UITableViewSource
        {
            private LockPasswordViewController _controller;

            public TableSource(LockPasswordViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    if(indexPath.Row == 0)
                    {
                        return _controller.MasterPasswordCell;
                    }
                }

                return new UITableViewCell();
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
                if(section == 0)
                {
                    return 1;
                }

                return 0;
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
