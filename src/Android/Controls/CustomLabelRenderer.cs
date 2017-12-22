using Android.Content;
using Bit.Android.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Label), typeof(CustomLabelRenderer))]
namespace Bit.Android.Controls
{
    public class CustomLabelRenderer : LabelRenderer
    {
        public CustomLabelRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);
            Control.SetMaxLines(int.MaxValue);
        }
    }
}
