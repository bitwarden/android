using Android.Content;
using Bit.Droid.Renderers.BoxedView;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Entry), typeof(CustomEntryBarRenderer))]
namespace Bit.Droid.Renderers.BoxedView
{
    public class CustomEntryBarRenderer : EntryRenderer
    {
        public CustomEntryBarRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Entry> e)
        {
            base.OnElementChanged(e);
            if(Control != null && e.NewElement != null)
            {
                Control.SetPadding(Control.PaddingLeft, Control.PaddingTop - 10, Control.PaddingRight,
                    Control.PaddingBottom + 20);
            }
        }
    }
}
