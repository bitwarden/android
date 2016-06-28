using System;
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
                var descriptor = UIFontDescriptor.PreferredBody;
                var pointSize = descriptor.PointSize;

                var size = view.FontSize;
                if(size == Device.GetNamedSize(NamedSize.Large, typeof(Label)))
                {
                    pointSize *= 1.7f;
                }
                else if(size == Device.GetNamedSize(NamedSize.Small, typeof(Label)))
                {
                    pointSize *= .8f;
                }
                else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(Label)))
                {
                    pointSize *= .6f;
                }

                UIFont.FromDescriptor(descriptor, pointSize);
            }
        }
    }
}
