using Bit.iOS.Core.Renderers;
using Bit.iOS.Core.Utilities;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(Picker), typeof(CustomPickerRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class CustomPickerRenderer : PickerRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Picker> e)
        {
            base.OnElementChanged(e);
            if (e.NewElement is Picker)
            {
                var descriptor = UIFontDescriptor.PreferredBody;
                Control.Font = UIFont.FromDescriptor(descriptor, descriptor.PointSize);
                iOSHelpers.SetBottomBorder(Control);
                UpdateKeyboardAppearance();
            }
        }

        private void UpdateKeyboardAppearance()
        {
            if (!ThemeHelpers.LightTheme)
            {
                Control.KeyboardAppearance = UIKeyboardAppearance.Dark;
            }
        }
    }
}
