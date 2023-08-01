using System.ComponentModel;
using Android.Content;
using Android.Views;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class ExtendedTimePickerRenderer : TimePickerRenderer
    {
        public ExtendedTimePickerRenderer(Context context)
            : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<TimePicker> e)
        {
            base.OnElementChanged(e);
            if (Control != null && Element is ExtendedTimePicker element)
            {
                // center text
                Control.Gravity = GravityFlags.CenterHorizontal;

                // use placeholder until NullableTime set 
                if (!element.NullableTime.HasValue)
                {
                    Control.Text = element.PlaceHolder;
                }
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
