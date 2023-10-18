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
            EntryHandler.Mapper.AppendToMapping("CustomEntryHandler", (handler, editor) =>
            {
                handler.PlatformView.ClearButtonMode = UITextFieldViewMode.WhileEditing;
                UpdateTintColor(handler, editor);
                iOSHelpers.SetBottomBorder(handler.PlatformView);

                if (!ThemeHelpers.LightTheme)
                {
                    handler.PlatformView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });

            EntryHandler.Mapper.AppendToMapping(nameof(IEntry.TextColor), UpdateTintColor);
        }

        private static void UpdateTintColor(IEntryHandler handler, IEntry editor)
        {
            handler.PlatformView.TintColor = editor.TextColor.ToPlatform();
        }
    }
}

