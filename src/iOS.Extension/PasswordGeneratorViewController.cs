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

namespace Bit.iOS.Extension
{
    public partial class PasswordGeneratorViewController : UIViewController
    {
        public PasswordGeneratorViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public SiteAddViewController Parent { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            base.ViewDidLoad();
        }

        partial void SelectBarButton_Activated(UIBarButtonItem sender)
        {
            throw new NotImplementedException();
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            throw new NotImplementedException();
        }

        public class TableSource : UITableViewSource
        {
            private PasswordGeneratorViewController _controller;

            public TableSource(PasswordGeneratorViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
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
                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
            }
        }
    }
}
