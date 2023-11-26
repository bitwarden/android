using Android.OS;
using Bit.App.Controls;
using Microsoft.Maui.Handlers;

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

            // WORKAROUND: There is an issue causing Multiline Labels that also have a LineBreakMode to not display text properly. (it truncates text on first line even with space available)
            // MAUI Github Issue: https://github.com/dotnet/maui/issues/14125 and https://github.com/dotnet/maui/pull/14918
            // When this gets fixed by MAUI these two Mapping below can be deleted, same for the UpdateMaxLines, TruncatedMultilineCustomLabel class and the equivalent Mappings on iOS
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(Label.LineBreakMode), UpdateMaxLines);
            Microsoft.Maui.Handlers.LabelHandler.Mapper.AppendToMapping(nameof(Label.MaxLines), UpdateMaxLines);
        }

        private static void UpdateMaxLines(ILabelHandler handler, ILabel label)
        {
            var textView = handler.PlatformView;
            if(label is TruncatedMultilineCustomLabel controlsLabel 
               && textView.Ellipsize == Android.Text.TextUtils.TruncateAt.End
               && controlsLabel.MaxLines != -1)
            {
                textView.SetMaxLines( controlsLabel.MaxLines );
            }
        }
    }
}
