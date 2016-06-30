using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedPicker), typeof(ExtendedPickerRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedPickerRenderer : PickerRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Picker> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ExtendedPicker;
            if(view != null)
            {
                var descriptor = UIFontDescriptor.PreferredBody;
                Control.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);

                SetBorder(view);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedPicker)Element;

            if(e.PropertyName == ExtendedPicker.HasBorderProperty.PropertyName)
            {
                SetBorder(view);
            }
        }

        private void SetBorder(ExtendedPicker view)
        {
            Control.BorderStyle = view.HasBorder ? UITextBorderStyle.RoundedRect : UITextBorderStyle.None;
        }
    }
}
