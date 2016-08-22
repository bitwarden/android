using System;
using System.ComponentModel;
using Bit.Android.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Button), typeof(CustomButtonRenderer))]
namespace Bit.Android.Controls
{
    public class CustomButtonRenderer : ButtonRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Button> e)
        {
            base.OnElementChanged(e);
            if(Control.TextSize == (float)Device.GetNamedSize(NamedSize.Default, typeof(Button)))
            {
                Control.TextSize = (float)Device.GetNamedSize(NamedSize.Medium, typeof(Button));
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
        }
    }
}
