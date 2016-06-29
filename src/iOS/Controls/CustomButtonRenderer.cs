using System;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

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
                var descriptor = UIFontDescriptor.PreferredBody;
                var pointSize = descriptor.PointSize;

                var size = view.FontSize;
                if(size == Device.GetNamedSize(NamedSize.Large, typeof(Button)))
                {
                    pointSize *= 1.4f;
                }
                else if(size == Device.GetNamedSize(NamedSize.Small, typeof(Button)))
                {
                    pointSize *= .8f;
                }
                else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(Button)))
                {
                    pointSize *= .6f;
                }

                Control.Font = UIFont.FromDescriptor(descriptor, pointSize);
            }
        }
    }
}
