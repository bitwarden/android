using Android.Graphics;
using Android.OS;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;

namespace Bit.App.Handlers
{
    public partial class StepperHandlerMappings
    {
        partial void SetupPlatform()
        {
            Microsoft.Maui.Handlers.StepperHandler.Mapper.AppendToMapping(nameof(ExtendedStepper.StepperBackgroundColor), (handler, stepper) =>
            {
                if (stepper is ExtendedStepper extStepper)
                {
                    if (Build.VERSION.SdkInt >= BuildVersionCodes.Q)
                    {
                        handler.PlatformView.GetChildAt(0)?.Background?.SetColorFilter(
                            new BlendModeColorFilter(extStepper.StepperBackgroundColor.ToAndroid(), Android.Graphics.BlendMode.Multiply));
                        handler.PlatformView.GetChildAt(1)?.Background?.SetColorFilter(
                            new BlendModeColorFilter(extStepper.StepperBackgroundColor.ToAndroid(), Android.Graphics.BlendMode.Multiply));
                    }
                    else
                    {
                        handler.PlatformView.GetChildAt(0)?.Background?.SetColorFilter(
                            extStepper.StepperBackgroundColor.ToAndroid(), PorterDuff.Mode.Multiply);
                        handler.PlatformView.GetChildAt(1)?.Background?.SetColorFilter(
                            extStepper.StepperBackgroundColor.ToAndroid(), PorterDuff.Mode.Multiply);
                    }
                }
            });

            Microsoft.Maui.Handlers.StepperHandler.Mapper.AppendToMapping(nameof(ExtendedStepper.StepperForegroundColor), (handler, stepper) =>
            {
                if (stepper is ExtendedStepper extStepper)
                {
                    var btn0 = handler.PlatformView.GetChildAt(0) as Android.Widget.Button;
                    btn0?.SetTextColor(extStepper.StepperForegroundColor.ToAndroid());
                    var btn1 = handler.PlatformView.GetChildAt(1) as Android.Widget.Button;
                    btn1?.SetTextColor(extStepper.StepperForegroundColor.ToAndroid());
                }
            });
        }
    }
}
