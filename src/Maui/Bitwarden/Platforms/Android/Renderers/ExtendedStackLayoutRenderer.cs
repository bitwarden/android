using Android.Content;
using Bit.App.Controls;
using Bit.App.Droid.Renderers;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    // TODO: Convert to handler
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
