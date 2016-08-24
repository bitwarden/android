using System;
using System.ComponentModel;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedButton), typeof(ExtendedButtonRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedButtonRenderer : CustomButtonRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Button> e)
        {
            base.OnElementChanged(e);
            SetPadding();
            SetUppercase();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if(e.PropertyName == ExtendedButton.PaddingProperty.PropertyName)
            {
                SetPadding();
            }
            else if(e.PropertyName == ExtendedButton.UppercaseProperty.PropertyName)
            {
                SetUppercase();
            }
        }

        private void SetPadding()
        {
            var element = Element as ExtendedButton;
            if(element != null)
            {
                Control.SetPadding(
                    (int)element.Padding.Left,
                    (int)element.Padding.Top,
                    (int)element.Padding.Right,
                    (int)element.Padding.Bottom);
            }
        }

        private void SetUppercase()
        {
            var element = Element as ExtendedButton;
            if(element != null && !string.IsNullOrWhiteSpace(element.Text))
            {
                if(element.Uppercase)
                {
                    element.Text = element.Text.ToUpperInvariant();
                }
                else
                {
                    Control.TransformationMethod = null;
                }
            }
        }
    }
}
