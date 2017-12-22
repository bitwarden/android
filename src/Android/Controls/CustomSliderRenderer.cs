using System;
using Bit.Android.Controls;
using Xamarin.Forms;
using Android.Content;
using Xamarin.Forms.Platform.Android;
using Android.Support.V4.Content.Res;

[assembly: ExportRenderer(typeof(Slider), typeof(CustomSliderRenderer))]
namespace Bit.Android.Controls
{
    public class CustomSliderRenderer : SliderRenderer
    {
        public CustomSliderRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Slider> e)
        {
            base.OnElementChanged(e);
            if(Control != null)
            {
                var thumb = ResourcesCompat.GetDrawable(Resources, Resource.Drawable.slider_thumb, null);
                Control.SetThumb(thumb);
            }
        }
    }
}
