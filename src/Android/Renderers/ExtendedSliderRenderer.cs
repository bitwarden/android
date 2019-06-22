using Android.Content;
using Android.Graphics.Drawables;
using Android.Support.V4.Content.Res;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedSlider), typeof(ExtendedSliderRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedSliderRenderer : SliderRenderer
    {
        public ExtendedSliderRenderer(Context context)
            : base(context)
        {}

        protected override void OnElementChanged(ElementChangedEventArgs<Slider> e)
        {
            base.OnElementChanged(e);
            if(Control != null && Element is ExtendedSlider view)
            {
                var t = ResourcesCompat.GetDrawable(Resources, Resource.Drawable.slider_thumb, null);
                if(t is GradientDrawable thumb)
                {
                    if(view.ThumbColor == Color.Default)
                    {
                        thumb.SetColor(Color.White.ToAndroid());
                    }
                    else
                    {
                        thumb.SetColor(view.ThumbColor.ToAndroid());
                    }
                    thumb.SetStroke(3, view.ThumbBorderColor.ToAndroid());
                    Control.SetThumb(thumb);
                }
            }
        }
    }
}
