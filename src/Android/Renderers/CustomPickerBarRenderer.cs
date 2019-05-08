using Android.Content;
using Bit.Droid.Renderers.BoxedView;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Picker), typeof(CustomPickerBarRenderer))]
namespace Bit.Droid.Renderers.BoxedView
{
    public class CustomPickerBarRenderer : PickerRenderer
    {
        public CustomPickerBarRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Picker> e)
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
