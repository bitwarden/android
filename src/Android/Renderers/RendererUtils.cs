using Android.Content.Res;
using Android.Graphics.Drawables;
using Android.Views;
using Xamarin.Forms;

namespace Bit.Droid.Renderers
{
    [Android.Runtime.Preserve(AllMembers = true)]
    public static class RendererUtils
    {
        public static GravityFlags ToAndroidVertical(this LayoutAlignment formsAlignment)
        {
            switch(formsAlignment)
            {
                case LayoutAlignment.Start:
                    return GravityFlags.Top;
                case LayoutAlignment.Center:
                    return GravityFlags.CenterVertical;
                case LayoutAlignment.End:
                    return GravityFlags.Bottom;
                default:
                    return GravityFlags.FillHorizontal;
            }
        }

        public static GravityFlags ToAndroidHorizontal(this LayoutAlignment formsAlignment)
        {
            switch(formsAlignment)
            {
                case LayoutAlignment.Start:
                    return GravityFlags.Start;
                case LayoutAlignment.Center:
                    return GravityFlags.CenterHorizontal;
                case LayoutAlignment.End:
                    return GravityFlags.End;
                default:
                    return GravityFlags.FillVertical;
            }
        }

        public static GravityFlags ToAndroidHorizontal(this Xamarin.Forms.TextAlignment formsAlignment)
        {
            switch(formsAlignment)
            {
                case Xamarin.Forms.TextAlignment.Start:
                    return GravityFlags.Left | GravityFlags.CenterVertical;
                case Xamarin.Forms.TextAlignment.Center:
                    return GravityFlags.Center | GravityFlags.CenterVertical;
                case Xamarin.Forms.TextAlignment.End:
                    return GravityFlags.Right | GravityFlags.CenterVertical;
                default:
                    return GravityFlags.Left | GravityFlags.CenterVertical;
            }
        }

        public static RippleDrawable CreateRipple(Android.Graphics.Color color, Drawable background = null)
        {
            if(background == null)
            {
                var mask = new ColorDrawable(Android.Graphics.Color.White);
                return new RippleDrawable(GetPressedColorSelector(color), null, mask);
            }
            return new RippleDrawable(GetPressedColorSelector(color), background, null);
        }

        public static ColorStateList GetPressedColorSelector(int pressedColor)
        {
            return new ColorStateList(
                new int[][]
                {
                    new int[]{}
                },
                new int[]
                {
                    pressedColor,
                });
        }
    }
}
