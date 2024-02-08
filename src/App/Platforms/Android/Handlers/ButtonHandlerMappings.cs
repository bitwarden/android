namespace Bit.App.Handlers
{
    public class ButtonHandlerMappings
    {
        public static void Setup()
        {
            Microsoft.Maui.Handlers.ButtonHandler.Mapper.AppendToMapping("CustomButtonHandler", (handler, button) =>
            {
                // WORKAROUND applied from https://github.com/dotnet/maui/issues/2918
                handler.PlatformView.StateListAnimator = null;
            });
        }
    }
}
