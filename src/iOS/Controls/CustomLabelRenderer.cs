using System;
using System.ComponentModel;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(Label), typeof(CustomLabelRenderer))]
namespace Bit.iOS.Controls
{
    public class CustomLabelRenderer : LabelRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as Label;
            if(Control != null && view != null)
            {
                UpdateFont();
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if(e.PropertyName == Label.TextColorProperty.PropertyName)
            {
                UpdateFont();
            }
            else if(e.PropertyName == Label.FontProperty.PropertyName)
            {
                UpdateFont();
            }
            else if(e.PropertyName == Label.TextProperty.PropertyName)
            {
                UpdateFont();
            }
            else if(e.PropertyName == Label.FormattedTextProperty.PropertyName)
            {
                UpdateFont();
            }
        }

        private void UpdateFont()
        {
            var pointSize = UIFontDescriptor.PreferredBody.PointSize;

            var size = Element.FontSize;
            if(size == Device.GetNamedSize(NamedSize.Large, typeof(Label)))
            {
                pointSize *= 1.3f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Small, typeof(Label)))
            {
                pointSize *= .8f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(Label)))
            {
                pointSize *= .6f;
            }
            else if(size != Device.GetNamedSize(NamedSize.Default, typeof(Label)))
            {
                // not using dynamic font sizes, return
                return;
            }

            Control.Font = UIFont.FromDescriptor(Element.Font.ToUIFont().FontDescriptor, pointSize);
        }
    }
}
