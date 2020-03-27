using System.ComponentModel;
using Bit.iOS.Renderers;
using Bit.iOS.Utilities;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(Label), typeof(CustomLabelRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomLabelRenderer : LabelRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);
            if (Control != null && e.NewElement is Label)
            {
                UpdateFont();
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if (e.PropertyName == Label.FontProperty.PropertyName ||
                e.PropertyName == Label.TextProperty.PropertyName ||
                e.PropertyName == Label.FormattedTextProperty.PropertyName)
            {
                UpdateFont();
            }
        }

        private void UpdateFont()
        {
            var pointSize = iOSHelpers.GetAccessibleFont<Label>(Element.FontSize);
            if (pointSize != null)
            {
                Control.Font = UIFont.FromDescriptor(Element.Font.ToUIFont().FontDescriptor, pointSize.Value);
            }
        }
    }
}
