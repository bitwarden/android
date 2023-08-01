using System.ComponentModel;
using Android.Content;
using Android.Content.Res;
using Android.Views.InputMethods;
using Bit.App.Droid.Utilities;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class CustomEditorRenderer : EditorRenderer
    {
        public CustomEditorRenderer(Context context)
            : base(context)
        { }
        
        // Workaround for issue described here:
        // https://github.com/xamarin/Xamarin.Forms/issues/8291#issuecomment-617456651
        protected override void OnAttachedToWindow()
        {
            base.OnAttachedToWindow();
            EditText.Enabled = false;
            EditText.Enabled = true;
        }

        protected override void OnElementChanged(ElementChangedEventArgs<Editor> e)
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
        
        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            
            if (e.PropertyName == Entry.TextColorProperty.PropertyName)
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
