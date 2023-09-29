using Android.Graphics.Drawables;
using AndroidX.Core.Content.Resources;
using Bit.App.Controls;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;

namespace Bit.App.Handlers
{
    public class SliderHandlerMappings
    {
        public static void Setup()
        {
            Microsoft.Maui.Handlers.SliderHandler.Mapper.AppendToMapping(nameof(ExtendedSlider.ThumbBorderColor), (handler, slider) =>
            {
                var t = ResourcesCompat.GetDrawable(handler.PlatformView.Resources, Resource.Drawable.slider_thumb, null);
                if (t is GradientDrawable thumb && slider is ExtendedSlider extSlider)
                {
                    // TODO: [MAUI-Migration]
                    //if (view.ThumbColor == Colors.Default)
                    //{
                    //    thumb.SetColor(Colors.White.ToAndroid());
                    //}
                    //else
                    //{
                    thumb.SetColor(extSlider.ThumbColor.ToAndroid());
                    //}
                    thumb.SetStroke(3, extSlider.ThumbBorderColor.ToAndroid());
                    handler.PlatformView.SetThumb(thumb);
                }
            });
        }
    }
}
