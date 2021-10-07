using System.ComponentModel;
using Android.Content;
using Android.Content.Res;
using Android.Graphics;
using Android.OS;
using Bit.Droid.Renderers;
using Bit.Droid.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Switch), typeof(CustomSwitchRenderer))]
namespace Bit.Droid.Renderers
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
            if (e.PropertyName == Switch.ThumbColorProperty.PropertyName)
            {
                UpdateColors();
            }
            else
            {
                base.OnElementPropertyChanged(sender, e);
            }
        }
        
        private void UpdateColors()
        {
            if (Control != null)
            {
                var states = new[]
                {
                    new[] { Android.Resource.Attribute.StateChecked }, // checked
                    new[] { -Android.Resource.Attribute.StateChecked } // unchecked
                };
                
                var thumbColors = new int[]
                {
                    ThemeHelpers.SwitchOnColor,
                    ThemeHelpers.SwitchThumbColor
                };
                Control.ThumbTintList = new ColorStateList(states, thumbColors);
                if (Build.VERSION.SdkInt >= BuildVersionCodes.Q)
                {
                    Control.ThumbTintBlendMode = BlendMode.Multiply;
                }
                else
                {
                    Control.ThumbTintMode = PorterDuff.Mode.Multiply;
                }
                
                var trackColors = new int[]
                {
                    ThemeHelpers.SwitchTrackOnColor,
                    ThemeHelpers.SwitchTrackOffColor
                };
                Control.TrackTintList = new ColorStateList(states, trackColors);
                if (Build.VERSION.SdkInt >= BuildVersionCodes.Q)
                {
                    Control.TrackTintBlendMode = BlendMode.Multiply;
                }
                else
                {
                    Control.TrackTintMode = PorterDuff.Mode.Multiply;
                }
            }
        }
    }
}
