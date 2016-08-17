using System;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;
using System.ComponentModel;
using Bit.App.Controls;

[assembly: ExportRenderer(typeof(ExtendedButton), typeof(ExtendedButtonRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedButtonRenderer : CustomButtonRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Button> e)
        {
            base.OnElementChanged(e);
            SetPadding();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if(e.PropertyName == ExtendedButton.PaddingProperty.PropertyName)
            {
                SetPadding();
            }
        }

        private void SetPadding()
        {
            var element = Element as ExtendedButton;
            if(element != null)
            {
                Control.ContentEdgeInsets = new UIEdgeInsets(
                    (int)element.Padding.Top,
                    (int)element.Padding.Left,
                    (int)element.Padding.Bottom,
                    (int)element.Padding.Right);
            }
        }
    }
}
