using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using Foundation;
using MobileCoreServices;
using Newtonsoft.Json;
using UIKit;
using XLabs.Ioc;

namespace Bit.iOS.Extension
{
    public partial class AddSiteViewController : UITableViewController
    {
        public AddSiteViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);

            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            View.BackgroundColor = new UIColor(red: 0.93f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            nameField.Text = Context.Url.Host;
            uriField.Text = Context.Url.ToString();

            tableView.RowHeight = UITableView.AutomaticDimension;
            tableView.EstimatedRowHeight = 44;
            tableView.Source = new TableSource (this);

            base.ViewDidLoad();
        }

        public override void ViewDidAppear(bool animated)
        {
            usernameCell.BecomeFirstResponder();
            base.ViewDidAppear(animated);
        }

        partial void UIBarButtonItem2289_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        partial void UIBarButtonItem2290_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        public class TableSource : UITableViewSource
        {
            private AddSiteViewController _controller;

            public TableSource (AddSiteViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell (UITableView tableView, NSIndexPath indexPath)
            {
                if (indexPath.Row == 0) {
                    return _controller.nameCell;
                } else if (indexPath.Row == 1) {
                    return _controller.uriCell;
                } else if (indexPath.Row == 2) {
                    return _controller.usernameCell;
                } else if (indexPath.Row == 3) {
                    return _controller.passwordCell;
                } else if (indexPath.Row == 4) {
                    return _controller.generatePasswordCell;
                }

                return new UITableViewCell();
            }

            public override nfloat GetHeightForRow (UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint RowsInSection (UITableView tableview, nint section)
            {
                if (section == 0) {
                    return 5;
                } else if (section == 1) {
                    return 2;
                } else {
                    return 1;
                }
            }

            public override nfloat GetHeightForHeader (UITableView tableView, nint section)
            {
                return UITableView.AutomaticDimension;
             }

            public override string TitleForHeader (UITableView tableView, nint section)
            {
                if (section == 0) {
                    return "Site Information";
                } else if (section == 2) {
                    return "Notes";
                }

                return " ";
            }
        }
    }
}
