using Bit.App.Controls;

namespace Bit.iOS.Core.Handlers
{
    public class LabelHandlerMappings
    {
        public static void Setup()
        {
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(ILabel.AutomationId), (handler, label) =>
            {
                if (label is CustomLabel customLabel)
                {
                    handler.PlatformView.AccessibilityIdentifier = customLabel.AutomationId;
                }
            });
        }
    }
}
