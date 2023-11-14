using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class EntryHandlerMappings
    {
        public static void Setup()
        {
            EntryHandler.Mapper.AppendToMapping("CustomEntryHandler", (handler, entry) =>
            {
                handler.PlatformView.ClearButtonMode = UITextFieldViewMode.WhileEditing;
                UpdateTintColor(handler, entry);
                iOSHelpers.SetBottomBorder(handler.PlatformView);

                if (!ThemeHelpers.LightTheme)
                {
                    handler.PlatformView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });

            EntryHandler.Mapper.AppendToMapping(nameof(IEntry.TextColor), UpdateTintColor);
        }

        private static void UpdateTintColor(IEntryHandler handler, IEntry entry)
        {
            // Note: the default black value is to avoid an error on the iOS extensions while lazy loading a view
            handler.PlatformView.TintColor = entry.TextColor.ToPlatform(Colors.Black);
        }
    }
}

