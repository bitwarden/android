using System.ComponentModel;
using Android.Content;
using Android.Content.Res;
using Android.Graphics.Drawables;
using Android.OS;
using AndroidX.Core.Content.Resources;
using Bit.App.Droid.Renderers;
using Bit.App.Droid.Utilities;
using Microsoft.Maui.Controls.Compatibility.Platform.Android.AppCompat;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Droid.Renderers
{
    public class CustomSwitchRenderer : SwitchRenderer
    {
        public CustomSwitchRenderer(Context context)
            : base(context) 
        {}
        
        protected override void OnElementChanged(ElementChangedEventArgs<Switch> e)
        {
            base.OnElementChanged(e);
            UpdateColors();
        }
        
        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            if (e.PropertyName == Switch.OnColorProperty.PropertyName)
            {
                UpdateColors();
            }
        }
        
        private void UpdateColors()
        {
            if (Build.VERSION.SdkInt <= BuildVersionCodes.LollipopMr1)
            {
                // Android 5.x doesn't support ThumbTintList, and using SwitchCompat on every version after 5.x
                // doesn't apply tinting the way we want. Let 5.x to do its own thing here.
                return;
            }
            if (Control != null)
            {
                Control.SetHintTextColor(ThemeHelpers.MutedColor);
                var t = ResourcesCompat.GetDrawable(Resources, Resource.Drawable.switch_thumb, null);
                if (t is GradientDrawable thumb)
                {
                    Control.ThumbDrawable = thumb;
                }
                var thumbStates = new[]
                {
                    new[] { Android.Resource.Attribute.StateChecked }, // checked
                    new[] { -Android.Resource.Attribute.StateChecked }, // unchecked
                };
                var thumbColors = new int[]
                {
                    ThemeHelpers.SwitchOnColor,
                    ThemeHelpers.SwitchThumbColor
                };
                Control.ThumbTintList = new ColorStateList(thumbStates, thumbColors);
            }
        }
    }
}
