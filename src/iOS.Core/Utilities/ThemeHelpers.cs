using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ThemeHelpers
    {
        public static bool LightTheme = true;
        public static bool UsingOsDarkTheme = false;
        public static UIColor SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#efeff4").ToUIColor();
        public static UIColor BackgroundColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
        public static UIColor MutedColor = Xamarin.Forms.Color.FromHex("#777777").ToUIColor();
        public static UIColor SuccessColor = Xamarin.Forms.Color.FromHex("#00a65a").ToUIColor();
        public static UIColor PrimaryColor = Xamarin.Forms.Color.FromHex("#3c8dbc").ToUIColor();
        public static UIColor TextColor = Xamarin.Forms.Color.FromHex("#000000").ToUIColor();
        public static UIColor PlaceholderColor = Xamarin.Forms.Color.FromHex("#d0d0d0").ToUIColor();
        public static UIColor SeparatorColor = Xamarin.Forms.Color.FromHex("#dddddd").ToUIColor();
        public static UIColor ListHeaderBackgroundColor = Xamarin.Forms.Color.FromHex("#efeff4").ToUIColor();
        public static UIColor NavBarBackgroundColor = Xamarin.Forms.Color.FromHex("#3c8dbc").ToUIColor();
        public static UIColor NavBarTextColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();

        public static void SetAppearance(string theme, bool usingOsDarkTheme)
        {
            UsingOsDarkTheme = usingOsDarkTheme;
            SetThemeVariables(theme);
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            UIStepper.Appearance.TintColor = MutedColor;
            if (!LightTheme)
            {
                UISwitch.Appearance.TintColor = MutedColor;
            }
        }

        public static void SetExtensionAppearance(string theme, bool usingOsDarkTheme)
        {
            SetAppearance(theme, usingOsDarkTheme);
            UIView.Appearance.BackgroundColor = BackgroundColor;
            UILabel.Appearance.TextColor = TextColor;
            UITextField.Appearance.TintColor = TextColor;
            UITextView.Appearance.TintColor = TextColor;
            UITextField.Appearance.BackgroundColor = BackgroundColor;
            UITextView.Appearance.BackgroundColor = BackgroundColor;
            UITableView.Appearance.BackgroundColor = BackgroundColor;
            UITableView.Appearance.SeparatorColor = SeparatorColor;
            UINavigationBar.Appearance.BackgroundColor = NavBarBackgroundColor;
            UINavigationBar.Appearance.BarTintColor = NavBarBackgroundColor;
            UINavigationBar.Appearance.TintColor = NavBarTextColor;
            UINavigationBar.Appearance.Translucent = false;
            UINavigationBar.Appearance.SetTitleTextAttributes(new UITextAttributes()
            {
                TextColor = NavBarTextColor
            });
            UIBarButtonItem.Appearance.TintColor = NavBarTextColor;
            UIButton.Appearance.TintColor = TextColor;
            UILabel.AppearanceWhenContainedIn(typeof(UITableViewHeaderFooterView)).TextColor = MutedColor;
        }

        private static void SetThemeVariables(string theme)
        {
            LightTheme = false;
            if (string.IsNullOrWhiteSpace(theme) && UsingOsDarkTheme)
            {
                theme = "dark";
            }

            if (theme == "dark")
            {
                var whiteColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
                MutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
                SuccessColor = Xamarin.Forms.Color.FromHex("#00a65a").ToUIColor();
                BackgroundColor = Xamarin.Forms.Color.FromHex("#303030").ToUIColor();
                SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#222222").ToUIColor();
                PrimaryColor = Xamarin.Forms.Color.FromHex("#52bdfb").ToUIColor();
                TextColor = whiteColor;
                PlaceholderColor = Xamarin.Forms.Color.FromHex("#707070").ToUIColor();
                SeparatorColor = Xamarin.Forms.Color.FromHex("#191919").ToUIColor();
                ListHeaderBackgroundColor = Xamarin.Forms.Color.FromHex("#222222").ToUIColor();
                NavBarBackgroundColor = Xamarin.Forms.Color.FromHex("#212121").ToUIColor();
                NavBarTextColor = whiteColor;
            }
            else if (theme == "black")
            {
                var blackColor = Xamarin.Forms.Color.FromHex("#000000").ToUIColor();
                var whiteColor = Xamarin.Forms.Color.FromHex("#ffffff").ToUIColor();
                MutedColor = Xamarin.Forms.Color.FromHex("#a3a3a3").ToUIColor();
                SuccessColor = Xamarin.Forms.Color.FromHex("#00a65a").ToUIColor();
                BackgroundColor = blackColor;
                SplashBackgroundColor = blackColor;
                PrimaryColor = Xamarin.Forms.Color.FromHex("#52bdfb").ToUIColor();
                TextColor = whiteColor;
                PlaceholderColor = Xamarin.Forms.Color.FromHex("#707070").ToUIColor();
                SeparatorColor = Xamarin.Forms.Color.FromHex("#282828").ToUIColor();
                ListHeaderBackgroundColor = blackColor;
                NavBarBackgroundColor = blackColor;
                NavBarTextColor = whiteColor;
            }
            else if (theme == "nord")
            {
                MutedColor = Xamarin.Forms.Color.FromHex("#d8dee9").ToUIColor();
                SuccessColor = Xamarin.Forms.Color.FromHex("#a3be8c").ToUIColor();
                BackgroundColor = Xamarin.Forms.Color.FromHex("#3b4252").ToUIColor();
                SplashBackgroundColor = Xamarin.Forms.Color.FromHex("#2e3440").ToUIColor();
                PrimaryColor = Xamarin.Forms.Color.FromHex("#81a1c1").ToUIColor();
                TextColor = Xamarin.Forms.Color.FromHex("#e5e9f0").ToUIColor();
                PlaceholderColor = Xamarin.Forms.Color.FromHex("#7b88a1").ToUIColor();
                SeparatorColor = Xamarin.Forms.Color.FromHex("#2e3440").ToUIColor();
                ListHeaderBackgroundColor = Xamarin.Forms.Color.FromHex("#2e3440").ToUIColor();
                NavBarBackgroundColor = Xamarin.Forms.Color.FromHex("#2e3440").ToUIColor();
                NavBarTextColor = Xamarin.Forms.Color.FromHex("#e5e9f0").ToUIColor();
            }
            else
            {
                LightTheme = true;
            }
        }
    }
}
