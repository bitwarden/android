using Bit.App.Droid.Utilities;

namespace Bit.App.Handlers
{
    public class PickerHandlerMappings
    {
        public static void Setup()
        {
            Microsoft.Maui.Handlers.PickerHandler.Mapper.AppendToMapping("CustomPickerHandler", (handler, picker) =>
            {
                handler.PlatformView.SetPadding(handler.PlatformView.PaddingLeft, handler.PlatformView.PaddingTop - 10,
                    handler.PlatformView.PaddingRight, handler.PlatformView.PaddingBottom + 20);
            });

            Microsoft.Maui.Handlers.PickerHandler.Mapper.AppendToMapping(nameof(IPicker.TextColor), (handler, picker) =>
            {
                handler.PlatformView.BackgroundTintList = ThemeHelpers.GetStateFocusedColors();
            });
        }
    }
}
