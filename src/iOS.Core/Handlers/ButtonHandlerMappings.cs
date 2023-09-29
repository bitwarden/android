using System;
using Bit.iOS.Core.Utilities;

namespace Bit.iOS.Core.Handlers
{
    public class ButtonHandlerMappings
    {
        public static void Setup()
        {
            // TODO: [Maui-Migration] Check if this is needed given that on MAUI FontAutoScalingEnabled is true by default.
            //Microsoft.Maui.Handlers.ButtonHandler.Mapper.AppendToMapping("CustomButtonHandler", (handler, button) =>
            //{
            //    var pointSize = iOSHelpers.GetAccessibleFont<Button>(button.FontSize);
            //    if (pointSize != null)
            //    {
            //        handler.PlatformView.Font = UIFont.FromDescriptor(Element.Font.ToUIFont().FontDescriptor, pointSize.Value);
            //    }
            //});
        }
    }
}
