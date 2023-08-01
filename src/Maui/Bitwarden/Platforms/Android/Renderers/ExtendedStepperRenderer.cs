using System.ComponentModel;
using Android.Content;
using Android.Graphics;
using Android.OS;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class ExtendedStepperRenderer : StepperRenderer
    {
        public ExtendedStepperRenderer(Context context)
            : base(context) 
        {}

        protected override void OnElementChanged(ElementChangedEventArgs<Stepper> e)
        {
            base.OnElementChanged(e);
            UpdateBgColor();
            UpdateFgColor();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if (e.PropertyName == ExtendedStepper.StepperBackgroundColorProperty.PropertyName)
            {
                UpdateBgColor();
            }
            else if (e.PropertyName == ExtendedStepper.StepperForegroundColorProperty.PropertyName)
            {
                UpdateFgColor();
            }
        }

        private void UpdateBgColor()
        {
            if (Control != null && Element is ExtendedStepper view)
            {
                if (Build.VERSION.SdkInt >= BuildVersionCodes.Q)
                {
                    Control.GetChildAt(0)?.Background?.SetColorFilter(
                        new BlendModeColorFilter(view.StepperBackgroundColor.ToAndroid(), Android.Graphics.BlendMode.Multiply));
                    Control.GetChildAt(1)?.Background?.SetColorFilter(
                        new BlendModeColorFilter(view.StepperBackgroundColor.ToAndroid(), Android.Graphics.BlendMode.Multiply));
                }
                else
                {
                    Control.GetChildAt(0)?.Background?.SetColorFilter(
                        view.StepperBackgroundColor.ToAndroid(), PorterDuff.Mode.Multiply);
                    Control.GetChildAt(1)?.Background?.SetColorFilter(
                        view.StepperBackgroundColor.ToAndroid(), PorterDuff.Mode.Multiply);
                }
            }
        }

        private void UpdateFgColor()
        {
            if (Control != null && Element is ExtendedStepper view)
            {
                var btn0 = Control.GetChildAt(0) as Android.Widget.Button;
                btn0?.SetTextColor(view.StepperForegroundColor.ToAndroid());
                var btn1 = Control.GetChildAt(1) as Android.Widget.Button;
                btn1?.SetTextColor(view.StepperForegroundColor.ToAndroid());
            }
        }
    }
}
