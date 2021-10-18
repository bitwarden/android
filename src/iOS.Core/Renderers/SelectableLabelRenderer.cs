using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(SelectableLabel), typeof(SelectableLabelRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class SelectableLabelRenderer : ViewRenderer<Label, UITextView>
    {
        UITextView uiTextView;

        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);

            if (e.OldElement != null || Element == null)
            {
                return;
            }

            if (Control == null)
            {
                uiTextView = new UITextView();
            }

            uiTextView.Selectable = true;
            uiTextView.Editable = false;
            uiTextView.ScrollEnabled = false;
            uiTextView.TextContainerInset = UIEdgeInsets.Zero;
            uiTextView.TextContainer.LineFragmentPadding = 0;
            uiTextView.BackgroundColor = UIColor.Clear;

            uiTextView.Text = Element.Text;
            uiTextView.TextColor = Element.TextColor.ToUIColor();
            switch (Element.FontAttributes)
            {
                case FontAttributes.None:
                    uiTextView.Font = UIFont.SystemFontOfSize(new nfloat(Element.FontSize));
                    break;
                case FontAttributes.Bold:
                    uiTextView.Font = UIFont.BoldSystemFontOfSize(new nfloat(Element.FontSize));
                    break;
                case FontAttributes.Italic:
                    uiTextView.Font = UIFont.ItalicSystemFontOfSize(new nfloat(Element.FontSize));
                    break;
                default:
                    uiTextView.Font = UIFont.BoldSystemFontOfSize(new nfloat(Element.FontSize));
                    break;
            }

            SetNativeControl(uiTextView);
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if (e.PropertyName == Label.TextProperty.PropertyName)
            {
                if (Control != null && Element != null && !string.IsNullOrWhiteSpace(Element.Text))
                {
                    uiTextView.Text = Element.Text;
                }
            }
        }

    }
}
