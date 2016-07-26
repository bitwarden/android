using System;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;
using System.ComponentModel;

[assembly: ExportRenderer(typeof(Button), typeof(CustomButtonRenderer))]
namespace Bit.iOS.Controls
{
    public class CustomButtonRenderer : ButtonRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Button> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as Button;
            if(Control != null && view != null)
            {
                UpdateFont();
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if(e.PropertyName == Button.FontProperty.PropertyName)
            {
                UpdateFont();
            }
        }

        private void UpdateFont()
        {
            var pointSize = UIFontDescriptor.PreferredBody.PointSize;

            var size = Element.FontSize;
            if(size == Device.GetNamedSize(NamedSize.Large, typeof(Button)))
            {
                pointSize *= 1.3f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Small, typeof(Button)))
            {
                pointSize *= .8f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(Button)))
            {
                pointSize *= .6f;
            }
            else if(size != Device.GetNamedSize(NamedSize.Default, typeof(Button)))
            {
                // not using dynamic font sizes, return
                return;
            }

            Control.Font = UIFont.FromDescriptor(Element.Font.ToUIFont().FontDescriptor, pointSize);
        }
    }
}
