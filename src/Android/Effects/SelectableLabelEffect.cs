using Android.Widget;
using Bit.Droid.Effects;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportEffect(typeof(SelectableLabelEffect), "SelectableLabelEffect")]
namespace Bit.Droid.Effects
{
    public class SelectableLabelEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Control is TextView textView)
            {
                textView.SetTextIsSelectable(true);
            }
        }

        protected override void OnDetached()
        {
        }
    }
}
