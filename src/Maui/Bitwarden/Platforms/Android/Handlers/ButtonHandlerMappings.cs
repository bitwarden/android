namespace Bit.App.Handlers
{
    public partial class ButtonHandlerMappings
    {
        partial void SetupPlatform()
        {
            Microsoft.Maui.Handlers.ButtonHandler.Mapper.AppendToMapping("CustomButtonHandler", (handler, button) =>
            {
                // WORKAROUND applied from https://github.com/dotnet/maui/issues/2918
                handler.PlatformView.StateListAnimator = null;
            });
        }
    }
}
