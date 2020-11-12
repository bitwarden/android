using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedSearchBar : SearchBar
    {
        public ExtendedSearchBar()
        {
            if (Device.RuntimePlatform == Device.iOS)
            {
                if (ThemeManager.UsingLightTheme)
                {
                    TextColor = Color.Black;
                }
            }
        }
    }
}
