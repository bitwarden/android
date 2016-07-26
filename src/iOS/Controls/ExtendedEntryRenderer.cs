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
                SetMaxLength(view);
                UpdateKeyboard();
                UpdateFont();

                if(view.AllowClear)
                {
                    Control.ClearButtonMode = UITextFieldViewMode.WhileEditing;
                }

                if(view.DisableAutocapitalize)
                {
                    Control.AutocapitalizationType = UITextAutocapitalizationType.None;
                }

                if(view.Autocorrect.HasValue)
                {
                    Control.AutocorrectionType = view.Autocorrect.Value ? UITextAutocorrectionType.Yes : UITextAutocorrectionType.No;
                }

                if(view.ReturnType.HasValue)
                {
                    switch(view.ReturnType.Value)
                    {
                        case App.Enums.ReturnType.Done:
                            Control.ReturnKeyType = UIReturnKeyType.Done;
                            break;
                        case App.Enums.ReturnType.Go:
                            Control.ReturnKeyType = UIReturnKeyType.Go;
                            break;
                        case App.Enums.ReturnType.Next:
                            Control.ReturnKeyType = UIReturnKeyType.Next;
                            break;
                        case App.Enums.ReturnType.Search:
                            Control.ReturnKeyType = UIReturnKeyType.Search;
                            break;
                        case App.Enums.ReturnType.Send:
                            Control.ReturnKeyType = UIReturnKeyType.Send;
                            break;
                        default:
                            Control.ReturnKeyType = UIReturnKeyType.Default;
                            break;
                    }
                }

                Control.ShouldReturn += (UITextField tf) =>
                {
                    view.InvokeCompleted();
                    return true;
                };
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedEntry)Element;

            if(e.PropertyName == ExtendedEntry.HasBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.HasOnlyBottomBorderProperty.PropertyName
                || e.PropertyName == ExtendedEntry.BottomBorderColorProperty.PropertyName)
            {
                SetBorder(view);
            }
            else if(e.PropertyName == Xamarin.Forms.InputView.KeyboardProperty.PropertyName)
            {
                UpdateKeyboard();
            }
            else if(e.PropertyName == Entry.FontAttributesProperty.PropertyName)
            {
                UpdateFont();
            }
            else if(e.PropertyName == Entry.FontFamilyProperty.PropertyName)
            {
                UpdateFont();
            }
            else if(e.PropertyName == Entry.FontSizeProperty.PropertyName)
            {
                UpdateFont();
            }
        }

        private void UpdateFont()
        {
            var descriptor = UIFontDescriptor.PreferredBody;
            var pointSize = descriptor.PointSize;

            var size = Element.FontSize;
            if(size == Device.GetNamedSize(NamedSize.Large, typeof(ExtendedEntry)))
            {
                pointSize *= 1.3f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Small, typeof(ExtendedEntry)))
            {
                pointSize *= .8f;
            }
            else if(size == Device.GetNamedSize(NamedSize.Micro, typeof(ExtendedEntry)))
            {
                pointSize *= .6f;
            }
            else if(size != Device.GetNamedSize(NamedSize.Default, typeof(ExtendedEntry)))
            {
                // not using dynamic font sizes, return
                return;
            }

            if(!string.IsNullOrWhiteSpace(Element.FontFamily))
            {
                Control.Font = UIFont.FromName(Element.FontFamily, pointSize);
            }
            else
            {
                Control.Font = UIFont.FromDescriptor(descriptor, pointSize);
            }
        }

        private void SetBorder(ExtendedEntry view)
        {
            if(view.HasOnlyBottomBorder)
            {
                var borderLayer = new CALayer();
                borderLayer.MasksToBounds = true;
                borderLayer.Frame = new CGRect(0f, Frame.Height / 2, Frame.Width * 2, 1f);
                borderLayer.BorderColor = view.BottomBorderColor.ToCGColor();
                borderLayer.BorderWidth = 1f;

                Control.Layer.AddSublayer(borderLayer);
                Control.BorderStyle = UITextBorderStyle.None;
            }
            else if(view.HasBorder)
            {
                Control.BorderStyle = UITextBorderStyle.RoundedRect;
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

        private void UpdateKeyboard()
        {
            if(Element.Keyboard == Keyboard.Numeric)
            {
                Control.KeyboardType = UIKeyboardType.NumberPad;
            }
            else
            {
                Control.ApplyKeyboard(Element.Keyboard);
            }
        }
    }
}
