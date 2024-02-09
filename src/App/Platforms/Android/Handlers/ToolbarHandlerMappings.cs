using Bit.Core.Resources.Localization;
using Microsoft.Maui.Handlers;

namespace Bit.App.Handlers
{
    public class ToolbarHandlerMappings
    {
        public static void Setup()
        {
            ToolbarHandler.Mapper.AppendToMapping(nameof(IToolbar.BackButtonVisible), (handler, view) =>
            {
                handler.PlatformView.NavigationContentDescription = AppResources.TapToGoBack;
            });

            ToolbarHandler.Mapper.AppendToMapping(nameof(Toolbar.BackButtonTitle), (handler, view) =>
            {
                handler.PlatformView.NavigationContentDescription = AppResources.TapToGoBack;
            });
        }
    }
}
