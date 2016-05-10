using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using CoreAnimation;
using CoreGraphics;
using Foundation;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedEntry), typeof(ExtendedEntryRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedEntryRenderer : EntryRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<Entry> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ExtendedEntry;
            if(view != null)
            {
                SetBorder(view);
                SetPlaceholderTextColor(view);
                SetMaxLength(view);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedEntry)Element;

            if(e.PropertyName == ExtendedEntry.HasBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.HasOnlyBottomBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.BorderColorProperty.PropertyName)
            {
                SetBorder(view);
            }
            if(e.PropertyName == ExtendedEntry.PlaceholderTextColorProperty.PropertyName)
            {
                SetPlaceholderTextColor(view);
            }
        }

        private void SetBorder(ExtendedEntry view)
        {
            if(view.HasBorder && view.HasOnlyBottomBorder)
            {
                var borderLayer = new CALayer();
                borderLayer.MasksToBounds = true;
                borderLayer.Frame = new CGRect(0f, Frame.Height / 2, Frame.Width, 1f);
                borderLayer.BorderColor = view.BorderColor.ToCGColor();
                borderLayer.BorderWidth = 1.0f;

                Control.Layer.AddSublayer(borderLayer);
                Control.BorderStyle = UITextBorderStyle.None;
            }
            else if(view.HasBorder)
            {
                Control.BorderStyle = UITextBorderStyle.Line;
            }
            else
            {
                Control.BorderStyle = UITextBorderStyle.None;
            }
        }

        private void SetMaxLength(ExtendedEntry view)
        {
            Control.ShouldChangeCharacters = (textField, range, replacementString) =>
            {
                var newLength = textField.Text.Length + replacementString.Length - range.Length;
                return newLength <= view.MaxLength;
            };
        }

        private void SetPlaceholderTextColor(ExtendedEntry view)
        {
            if(string.IsNullOrEmpty(view.Placeholder) == false && view.PlaceholderTextColor != Color.Default)
            {
                var placeholderString = new NSAttributedString(
                    view.Placeholder,
                    new UIStringAttributes()
                    {
                        ForegroundColor = view.PlaceholderTextColor.ToUIColor()
                    });

                Control.AttributedPlaceholder = placeholderString;
            }
        }
    }
}
