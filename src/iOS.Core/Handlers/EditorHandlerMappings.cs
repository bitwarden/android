using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class EditorHandlerMappings
    {
        public static void Setup()
        {
            EditorHandler.Mapper.AppendToMapping("CustomEditorHandler", (handler, editor) =>
            {
                var descriptor = UIFontDescriptor.PreferredBody;
                handler.PlatformView.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
                // Remove padding
                handler.PlatformView.TextContainerInset = new UIEdgeInsets(0, 0, 0, 0);
                handler.PlatformView.TextContainer.LineFragmentPadding = 0;
                UpdateTintColor(handler, editor);

                if (!Utilities.ThemeHelpers.LightTheme)
                {
                    handler.PlatformView.KeyboardAppearance = UIKeyboardAppearance.Dark;
                }
            });

            EditorHandler.Mapper.AppendToMapping(nameof(IEditor.TextColor), UpdateTintColor);
        }

        private static void UpdateTintColor(IEditorHandler handler, IEditor editor)
        {
            // Note: the default black value is to avoid an error on the iOS extensions while lazy loading a view
            handler.PlatformView.TintColor = editor.TextColor.ToPlatform(Colors.Black);
        }
    }
}
