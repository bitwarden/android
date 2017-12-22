using System;
using System.ComponentModel;
using Android.Content;
using Bit.Android.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Button), typeof(CustomButtonRenderer))]
namespace Bit.Android.Controls
{
    public class CustomButtonRenderer : ButtonRenderer
    {
        public CustomButtonRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Button> e)
        {
            base.OnElementChanged(e);
            if(Control.TextSize == (float)Device.GetNamedSize(NamedSize.Default, typeof(Button)))
            {
                Control.TextSize = (float)Device.GetNamedSize(NamedSize.Medium, typeof(Button));
            }

            // This will prevent all screen overlay apps from being able to interact with buttons.
            // Ex: apps that change the screen color for "night mode"
            // Control.FilterTouchesWhenObscured = true;
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
        }
    }
}
