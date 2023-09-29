using Android.Views.InputMethods;
using Bit.App.Droid.Utilities;

namespace Bit.App.Handlers
{
    public class EditorHandlerMappings
    {
        public static void Setup()
        {
            Microsoft.Maui.Handlers.EditorHandler.Mapper.AppendToMapping("CustomEditorHandler", (handler, editor) =>
            {
                handler.PlatformView.SetPadding(handler.PlatformView.PaddingLeft, handler.PlatformView.PaddingTop - 10, handler.PlatformView.PaddingRight,
                    handler.PlatformView.PaddingBottom + 20);
                handler.PlatformView.ImeOptions = handler.PlatformView.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                    (ImeAction)ImeFlags.NoExtractUi;
            });

            Microsoft.Maui.Handlers.EntryHandler.Mapper.AppendToMapping(nameof(IEditor.TextColor), (handler, editor) =>
            {
                handler.PlatformView.BackgroundTintList = ThemeHelpers.GetStateFocusedColors();
            });
        }
    }
}
