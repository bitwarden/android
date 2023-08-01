using System.ComponentModel;
using Android.Content;
using Android.Content.Res;
using Android.Graphics;
using Android.Text;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.App.Droid.Utilities;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;
using Microsoft.Maui.Platform;

namespace Bit.App.Droid.Renderers
{
    public class CustomEntryRenderer : EntryRenderer
    {
        public CustomEntryRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Entry> e)
        {
            base.OnElementChanged(e);
            UpdateBorderColor();
            if (Control != null && e.NewElement != null)
            {
                Control.SetPadding(Control.PaddingLeft, Control.PaddingTop - 10, Control.PaddingRight,
                    Control.PaddingBottom + 20);
                Control.ImeOptions = Control.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                    (ImeAction)ImeFlags.NoExtractUi;
            }   
        }

        // Workaround for bug preventing long-press -> copy/paste on Android 11
        // See https://issuetracker.google.com/issues/37095917
        protected override void OnAttachedToWindow()
        {
            base.OnAttachedToWindow();
            Control.Enabled = false;
            Control.Enabled = true;
        }

        // Workaround for failure to disable text prediction on non-password fields
        // see https://github.com/xamarin/Xamarin.Forms/issues/10857
        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            
            // Check if changed property is "IsPassword", otherwise ignore
            if (e.PropertyName == Entry.IsPasswordProperty.PropertyName)
            {
                // Check if field type is text, otherwise ignore (numeric passwords, etc.)
                EditText.InputType = Element.Keyboard.ToInputType();
                bool isText = (EditText.InputType & InputTypes.ClassText) == InputTypes.ClassText,
                    isNumber = (EditText.InputType & InputTypes.ClassNumber) == InputTypes.ClassNumber;
                if (isText || isNumber)
                {
                    if (Element.IsPassword)
                    {
                        // Element is a password field, set inputType to TextVariationPassword which disables
                        // predictive text by default
                        EditText.InputType = EditText.InputType |
                            (isText ? InputTypes.TextVariationPassword : InputTypes.NumberVariationPassword);
                    }
                    else
                    {
                        // Element is not a password field, set inputType to TextVariationVisiblePassword to
                        // disable predictive text while still displaying the content.
                        EditText.InputType = EditText.InputType |
                            (isText ? InputTypes.TextVariationVisiblePassword : InputTypes.NumberVariationNormal);
                    }
                    
                    // The workaround above forces a reset of the style properties, so we need to re-apply the font.
                    // see https://xamarin.github.io/bugzilla-archives/33/33666/bug.html
                    var typeface = Typeface.CreateFromAsset(Context.Assets, "RobotoMono_Regular.ttf");
                    if (Control is TextView label)
                    {
                        label.Typeface = typeface;
                    }
                }
            }
            else if (e.PropertyName == Entry.TextColorProperty.PropertyName)
            {
                UpdateBorderColor();
            }
        }

        private void UpdateBorderColor()
        {
            if (Control != null)
            {
                var states = new[]
                {
                    new[] { Android.Resource.Attribute.StateFocused }, // focused
                    new[] { -Android.Resource.Attribute.StateFocused }, // unfocused
                };
                var colors = new int[]
                {
                    ThemeHelpers.PrimaryColor, 
                    ThemeHelpers.MutedColor
                };
                Control.BackgroundTintList = new ColorStateList(states, colors);
            }
        }
    }
}
