using Android.Content;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedStackLayout), typeof(ExtendedStackLayoutRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedStackLayoutRenderer : ViewRenderer
    {
        public ExtendedStackLayoutRenderer(Context context) : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<View> elementChangedEvent)
        {
            base.OnElementChanged(elementChangedEvent);
            if (elementChangedEvent.NewElement != null)
            {
                SetBackgroundResource(Resource.Drawable.list_item_bg);
            }
        }
    }
}
