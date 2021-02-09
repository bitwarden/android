using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedTimePicker), typeof(ExtendedTimePickerRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class ExtendedTimePickerRenderer : TimePickerRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<TimePicker> e)
        {
            base.OnElementChanged(e);
            if (Control != null && Element is ExtendedTimePicker element)
            {
                // center text
                Control.TextAlignment = UITextAlignment.Center;

                // use placeholder until NullableTime set 
                if (!element.NullableTime.HasValue)
                {
                    Control.Text = element.PlaceHolder;
                }

                // force use of wheel picker on iOS 14+
                // TODO remove this when we upgrade to X.F 5 SR-1
                // https://github.com/xamarin/Xamarin.Forms/issues/12258#issuecomment-700168665
                try
                {
                    if (UIDevice.CurrentDevice.CheckSystemVersion(13, 2))
                    {
                        var picker = (UIDatePicker)Control.InputView;
                        picker.PreferredDatePickerStyle = UIDatePickerStyle.Wheels;
                    }
                }
                catch { }
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == TimePicker.TimeProperty.PropertyName ||
                e.PropertyName == TimePicker.FormatProperty.PropertyName)
            {
                if (Control != null && Element is ExtendedTimePicker element)
                {
                    if (Element.Format == element.PlaceHolder)
                    {
                        Control.Text = element.PlaceHolder;
                        return;
                    }
                }
            }
            base.OnElementPropertyChanged(sender, e);
        }
    }
}
