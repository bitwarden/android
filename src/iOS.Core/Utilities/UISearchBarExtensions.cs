using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class UISearchBarExtensions
    {
        public static void ApplyDarkThemesColors(this UISearchBar searchBar)
        {
            var versionParts = UIDevice.CurrentDevice.SystemVersion.Split('.');
            if (versionParts.Length > 0 && int.TryParse(versionParts[0], out var version))
            {
                if (version >= 13)
                {
                    searchBar.SearchTextField.TextColor = UIColor.White;
                    searchBar.SearchTextField.LeftView.TintColor = UIColor.White;
                }
            }
        }
    }
}
