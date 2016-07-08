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
            tableView.RowHeight = UITableView.AutomaticDimension;
            tableView.EstimatedRowHeight = 44;

            base.ViewDidLoad();
        }

        partial void UIBarButtonItem2289_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        partial void UIBarButtonItem2290_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }
    }
}
