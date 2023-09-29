using Android.OS;
using Bit.App.Controls;

namespace Bit.App.Handlers
{
    public class LabelHandlerMappings
    {
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Interoperability", "CA1416:Validate platform compatibility", Justification = "<Pending>")]
        public static void Setup()
        {
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping("CustomLabelHandler", (handler, label) =>
            {
                if (label is CustomLabel customLabel && customLabel.FontWeight.HasValue && Build.VERSION.SdkInt >= BuildVersionCodes.P)
                {
                    handler.PlatformView.Typeface = Android.Graphics.Typeface.Create(null, customLabel.FontWeight.Value, false);
                    return;
                }

                if (label is SelectableLabel)
                {
                    handler.PlatformView.SetTextIsSelectable(true);
                }
            });

            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(ILabel.AutomationId), (handler, label) =>
            {
                handler.PlatformView.ContentDescription = label.AutomationId;
            });
        }
    }
}
