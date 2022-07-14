using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class UISearchBarExtensions
    {
        public static void UpdateThemeIfNeeded(this UISearchBar searchBar)
        {
            if (!ThemeHelpers.LightTheme)
            {
                searchBar.KeyboardAppearance = UIKeyboardAppearance.Dark;
                if (UIDevice.CurrentDevice.CheckSystemVersion(13, 0))
                {
                    searchBar.SearchTextField.TextColor = UIColor.White;
                    searchBar.SearchTextField.LeftView.TintColor = UIColor.White;
                }
            }
        }
    }
}
