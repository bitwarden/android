using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Handlers;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class SearchBarHandlerMappings
    {
        public static void Setup()
        {
            SearchBarHandler.Mapper.AppendToMapping("CustomSearchBarHandler", (handler, searchBar) =>
            {
                if (!ThemeHelpers.LightTheme)
                {
                    handler.PlatformView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });
        }
    }
}
