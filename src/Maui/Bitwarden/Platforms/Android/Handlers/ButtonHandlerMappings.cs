using Bit.App.Controls;

namespace Bit.App.Handlers
{
    public partial class ButtonHandlerMappings
    {
        partial void SetupPlatform()
        {
            Microsoft.Maui.Handlers.ButtonHandler.Mapper.AppendToMapping("CustomButtonHandler", (handler, button) =>
            {
                if (button is IconButton || button is MiButton)
                {
                    handler.PlatformView.SetBackgroundResource(0);
                }
            });
        }
    }
}
