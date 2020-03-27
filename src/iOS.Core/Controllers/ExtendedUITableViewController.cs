using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUITableViewController : UITableViewController
    {
        public ExtendedUITableViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
        }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
            if (View != null)
            {
                View.BackgroundColor = ThemeHelpers.BackgroundColor;
            }
            if (TableView != null)
            {
                TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
                TableView.SeparatorColor = ThemeHelpers.SeparatorColor;
            }
            if (NavigationController?.NavigationBar != null)
            {
                NavigationController.NavigationBar.BarTintColor = ThemeHelpers.NavBarBackgroundColor;
                NavigationController.NavigationBar.BackgroundColor = ThemeHelpers.NavBarBackgroundColor;
                NavigationController.NavigationBar.TintColor = ThemeHelpers.NavBarTextColor;
                NavigationController.NavigationBar.TitleTextAttributes = new UIStringAttributes
                {
                    ForegroundColor = ThemeHelpers.NavBarTextColor
                };
            }
        }
    }
}
