using Bit.App.Controls;
using Microsoft.Maui.Handlers;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    public class StepperHandlerMappings
    {
        public static void Setup()
        {
            StepperHandler.Mapper.AppendToMapping("CustomStepperHandler", UpdateFgColor);

            StepperHandler.Mapper.AppendToMapping(nameof(ExtendedStepper.StepperForegroundColor), UpdateFgColor);
        }

        private static void UpdateFgColor(IStepperHandler handler, IStepper stepper)
        {
            if (stepper is ExtendedStepper extStepper)
            {
                if (UIDevice.CurrentDevice.CheckSystemVersion(13, 0))
                {
                    // https://developer.apple.com/forums/thread/121495
                    handler.PlatformView.SetIncrementImage(handler.PlatformView.GetIncrementImage(UIControlState.Normal), UIControlState.Normal);
                    handler.PlatformView.SetDecrementImage(handler.PlatformView.GetDecrementImage(UIControlState.Normal), UIControlState.Normal);
                }
                handler.PlatformView.TintColor = extStepper.StepperForegroundColor.ToPlatform();
            }
        }
    }
}
