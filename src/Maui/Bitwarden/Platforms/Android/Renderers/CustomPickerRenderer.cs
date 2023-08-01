using System.ComponentModel;
using Android.Content;
using Android.Content.Res;
using Bit.App.Droid.Utilities;
using Bit.App.Droid.Renderers;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Compatibility.Platform.Android.AppCompat;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class CustomPickerRenderer : PickerRenderer
    {
        public CustomPickerRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<Picker> e)
        {
            base.OnElementChanged(e);
            UpdateBorderColor();
            if (Control != null && e.NewElement != null)
            {
                Control.SetPadding(Control.PaddingLeft, Control.PaddingTop - 10, Control.PaddingRight,
                    Control.PaddingBottom + 20);
            }
        }
        
        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            
            if (e.PropertyName == Picker.TextColorProperty.PropertyName)
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
