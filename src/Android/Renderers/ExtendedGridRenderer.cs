using Android.Content;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedGrid), typeof(ExtendedGridRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedGridRenderer : ViewRenderer
    {
        private static int? _bgResId;

        public ExtendedGridRenderer(Context context) : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<View> elementChangedEvent)
        {
            base.OnElementChanged(elementChangedEvent);
            if (elementChangedEvent.NewElement != null)
            {
                SetBackgroundResource(GetBgResId());
            }
        }

        private int GetBgResId()
        {
            if (_bgResId == null)
            {
                if (ThemeManager.GetTheme(true) == "nord")
                {
                    _bgResId = Resource.Drawable.list_item_bg_nord;
                }
                else
                {
                    _bgResId ??= ThemeManager.UsingLightTheme ? Resource.Drawable.list_item_bg :
                        Resource.Drawable.list_item_bg_dark;
                }
            }
            return _bgResId.Value;
        }
    }
}
