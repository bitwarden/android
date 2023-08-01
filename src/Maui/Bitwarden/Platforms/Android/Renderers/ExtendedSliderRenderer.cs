using System.ComponentModel;
using Android.Content;
using Android.Graphics.Drawables;
using AndroidX.Core.Content.Resources;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class ExtendedSliderRenderer : SliderRenderer
    {
        public ExtendedSliderRenderer(Context context)
            : base(context)
        {}

        protected override void OnElementChanged(ElementChangedEventArgs<Slider> e)
        {
            base.OnElementChanged(e);
            UpdateColor();
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if (e.PropertyName == ExtendedSlider.ThumbBorderColorProperty.PropertyName)
            {
                UpdateColor();
            }
        }
        
        private void UpdateColor()
        {
            if (Control != null && Element is ExtendedSlider view)
            {
                var t = ResourcesCompat.GetDrawable(Resources, Resource.Drawable.slider_thumb, null);
                if (t is GradientDrawable thumb)
                {
                    // TODO: [MAUI-Migration]
                    //if (view.ThumbColor == Colors.Default)
                    //{
                    //    thumb.SetColor(Colors.White.ToAndroid());
                    //}
                    //else
                    //{
                        thumb.SetColor(view.ThumbColor.ToAndroid());
                    //}
                    thumb.SetStroke(3, view.ThumbBorderColor.ToAndroid());
                    Control.SetThumb(thumb);
                }
            }
        }
    }
}
