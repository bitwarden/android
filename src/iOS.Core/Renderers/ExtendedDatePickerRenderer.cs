using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedDatePicker), typeof(ExtendedDatePickerRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class ExtendedDatePickerRenderer : DatePickerRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<DatePicker> e)
        {
            base.OnElementChanged(e);
            if (Control != null && Element is ExtendedDatePicker element)
            {
                // center text
                Control.TextAlignment = UITextAlignment.Center;

                // use placeholder until NullableDate set 
                if (!element.NullableDate.HasValue)
                {
                    Control.Text = element.PlaceHolder;
                }
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == DatePicker.DateProperty.PropertyName ||
                e.PropertyName == DatePicker.FormatProperty.PropertyName)
            {
                if (Control != null && Element is ExtendedDatePicker element)
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
