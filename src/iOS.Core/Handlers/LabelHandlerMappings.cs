using Bit.App.Controls;
using Microsoft.Maui.Handlers;
using UIKit;

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

            // WORKAROUND: There is an issue causing Multiline Labels that also have a LineBreakMode to not display text properly. (it truncates text on first line even with space available)
            // MAUI Github Issue: https://github.com/dotnet/maui/issues/14125 and https://github.com/dotnet/maui/pull/14918
            // When this gets fixed by MAUI these two Mapping below can be deleted, same for the UpdateMaxLines, TruncatedMultilineCustomLabel class and the equivalent Mappings on Android
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(Label.LineBreakMode), UpdateMaxLines);
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(Label.MaxLines), UpdateMaxLines);
        }

        private static void UpdateMaxLines(ILabelHandler handler, ILabel label)
        {
            var textView = handler.PlatformView;
            if(label is TruncatedMultilineCustomLabel controlsLabel
               && textView.LineBreakMode == UILineBreakMode.TailTruncation
               && controlsLabel.MaxLines != -1)
            {
                textView.Lines = controlsLabel.MaxLines;
            }  
        }
    }
}
