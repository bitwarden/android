using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Handlers;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class PickerHandlerMappings
    {
        public static void Setup()
        {
            PickerHandler.Mapper.AppendToMapping("CustomPickerHandler", (handler, picker) =>
            {
                var descriptor = UIFontDescriptor.PreferredBody;
                handler.PlatformView.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
                iOSHelpers.SetBottomBorder(handler.PlatformView);

                if (!ThemeHelpers.LightTheme)
                {
                    handler.PlatformView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });
        }
    }
}
