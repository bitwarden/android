using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUIViewController : UIViewController
    {
        public Action DismissModalAction { get; set; }

        public ExtendedUIViewController()
        {
        }

        public ExtendedUIViewController(IntPtr handle)
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
            UpdateNavigationBarTheme();
        }

        protected virtual void UpdateNavigationBarTheme()
        {
            UpdateNavigationBarTheme(NavigationController?.NavigationBar);
        }

        protected void UpdateNavigationBarTheme(UINavigationBar navBar)
        {
            if (navBar is null)
            {
                return;
            }

            navBar.BarTintColor = ThemeHelpers.NavBarBackgroundColor;
            navBar.BackgroundColor = ThemeHelpers.NavBarBackgroundColor;
            navBar.TintColor = ThemeHelpers.NavBarTextColor;
            navBar.TitleTextAttributes = new UIStringAttributes
            {
                ForegroundColor = ThemeHelpers.NavBarTextColor
            };
        }
    }
}
