using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using CoreAnimation;
using CoreGraphics;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedPicker), typeof( ExtendedPickerRenderer ) )]
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
                SetBorder(view);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedPicker)Element;

            if(e.PropertyName == ExtendedPicker.HasBorderProperty.PropertyName
                || e.PropertyName == ExtendedPicker.HasOnlyBottomBorderProperty.PropertyName
                || e.PropertyName == ExtendedPicker.BottomBorderColorProperty.PropertyName)
            {
                SetBorder(view);
            }
        }

        private void SetBorder( ExtendedPicker view )
        {
            if(view.HasOnlyBottomBorder)
            {
                var borderLayer = new CALayer();
                borderLayer.MasksToBounds = true;
                borderLayer.Frame = new CGRect( 0f, Frame.Height / 2, Frame.Width * 2, 1f );
                borderLayer.BorderColor = view.BottomBorderColor.ToCGColor();
                borderLayer.BorderWidth = 1f;

                Control.Layer.AddSublayer( borderLayer );
                Control.BorderStyle = UITextBorderStyle.None;
            }
            else if(view.HasBorder)
            {
                Control.BorderStyle = UITextBorderStyle.RoundedRect;
            }
            else
            {
                Control.BorderStyle = UITextBorderStyle.None;
            }
        }
    }
}
