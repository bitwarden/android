using System.ComponentModel;
using Bit.iOS.Renderers;
using Bit.iOS.Utilities;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(Entry), typeof(CustomEntryRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomEntryRenderer : EntryRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Entry> e)
        {
            base.OnElementChanged(e);
            if (Control != null && e.NewElement is Entry)
            {
                Control.ClearButtonMode = UITextFieldViewMode.WhileEditing;
                UpdateTintColor();
                UpdateFontSize();
                UpdateKeyboardAppearance();
                iOSHelpers.SetBottomBorder(Control);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if (e.PropertyName == Entry.FontAttributesProperty.PropertyName ||
                e.PropertyName == Entry.FontFamilyProperty.PropertyName ||
                e.PropertyName == Entry.FontSizeProperty.PropertyName)
            {
                UpdateFontSize();
            }
            else if (e.PropertyName == Entry.TextColorProperty.PropertyName)
            {
                UpdateTintColor();
            }
        }

        private void UpdateFontSize()
        {
            var pointSize = iOSHelpers.GetAccessibleFont<Entry>(Element.FontSize);
            if (pointSize != null)
            {
                if (!string.IsNullOrWhiteSpace(Element.FontFamily))
                {
                    Control.Font = UIFont.FromName(Element.FontFamily, pointSize.Value);
                }
                else
                {
                    Control.Font = UIFont.FromDescriptor(UIFontDescriptor.PreferredBody, pointSize.Value);
                }
            }
        }

        private void UpdateTintColor()
        {
            Control.TintColor = Element.TextColor.ToUIColor();
        }

        private void UpdateKeyboardAppearance()
        {
            if (!Core.Utilities.ThemeHelpers.LightTheme)
            {
                Control.KeyboardAppearance = UIKeyboardAppearance.Dark;
            }
        }
    }
}
