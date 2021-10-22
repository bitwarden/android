using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedEditor), typeof(ExtendedEditorRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class ExtendedEditorRenderer : EditorRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Editor> e)
        {
            base.OnElementChanged(e);

            if (Control != null && Element is ExtendedEditor view)
            {
                Control.ScrollEnabled = view.IsScrollEnabled;
            }
        }
    }
}
