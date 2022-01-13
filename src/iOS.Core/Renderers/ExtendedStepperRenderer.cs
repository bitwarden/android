using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedStepper), typeof(ExtendedStepperRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class ExtendedStepperRenderer : StepperRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Stepper> e)
        {
            base.OnElementChanged(e);
            UpdateFgColor();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == ExtendedStepper.StepperForegroundColorProperty.PropertyName)
            {
                UpdateFgColor();
            }
            else
            {
                base.OnElementPropertyChanged(sender, e);
            }
        }

        private void UpdateFgColor()
        {
            if (Control != null && Element is ExtendedStepper view)
            {
                if (UIDevice.CurrentDevice.CheckSystemVersion(13, 0))
                {
                    // https://developer.apple.com/forums/thread/121495
                    Control.SetIncrementImage(Control.GetIncrementImage(UIControlState.Normal), UIControlState.Normal);
                    Control.SetDecrementImage(Control.GetDecrementImage(UIControlState.Normal), UIControlState.Normal);
                }
                Control.TintColor = view.StepperForegroundColor.ToUIColor();
            }
        }
    }
}
