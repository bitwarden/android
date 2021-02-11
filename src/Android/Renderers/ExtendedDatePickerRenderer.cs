using System.ComponentModel;
using Android.Content;
using Android.Views;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedDatePicker), typeof(ExtendedDatePickerRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedDatePickerRenderer : DatePickerRenderer
    {
        public ExtendedDatePickerRenderer(Context context)
            : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<DatePicker> e)
        {
            base.OnElementChanged(e);
            if (Control != null && Element is ExtendedDatePicker element)
            {
                // center text
                Control.Gravity = GravityFlags.CenterHorizontal;

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
