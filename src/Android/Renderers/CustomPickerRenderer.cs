using Android.Content;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Picker), typeof(CustomPickerRenderer))]
namespace Bit.Droid.Renderers
{
    public class CustomPickerRenderer : PickerRenderer
    {
        public CustomPickerRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Picker> e)
        {
            base.OnElementChanged(e);
            if (Control != null && e.NewElement != null)
            {
                Control.SetPadding(Control.PaddingLeft, Control.PaddingTop - 10, Control.PaddingRight,
                    Control.PaddingBottom + 20);
            }
        }
    }
}
