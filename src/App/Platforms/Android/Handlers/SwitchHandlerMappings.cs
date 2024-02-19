using Android.Content.Res;
using Android.Graphics.Drawables;
using Android.OS;
using AndroidX.Core.Content.Resources;
using AndroidX.Core.Graphics;
using Bit.App.Droid.Utilities;
using Bit.App.Utilities;
using Microsoft.Maui.Platform;

namespace Bit.App.Handlers
{
    public class SwitchHandlerMappings
    {
        public static void Setup()
        {
            if (Build.VERSION.SdkInt <= BuildVersionCodes.LollipopMr1)
            {
                // Android 5.x doesn't support ThumbTintList, and using SwitchCompat on every version after 5.x
                // doesn't apply tinting the way we want. Let 5.x to do its own thing here.
                return;
            }

            Microsoft.Maui.Handlers.SwitchHandler.Mapper.AppendToMapping(nameof(ISwitch.ThumbColor), (handler, mauiSwitch) =>
            {
                handler.PlatformView.SetHintTextColor(ThemeHelpers.MutedColor);
                var t = ResourcesCompat.GetDrawable(handler.PlatformView.Resources, Resource.Drawable.switch_thumb, null);
                if (t is GradientDrawable thumb)
                {
                    handler.PlatformView.ThumbDrawable = thumb;
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
                handler.PlatformView.ThumbTintList = new ColorStateList(thumbStates, thumbColors);
            });

            Microsoft.Maui.Handlers.SwitchHandler.Mapper.AppendToMapping(nameof(ISwitch.TrackColor), (handler, mauiSwitch) =>
            {
                var trackStates = new[]
                {
                    new[] { Android.Resource.Attribute.StateChecked }, // checked
                    new[] { -Android.Resource.Attribute.StateChecked }, // unchecked
                };

                var selectedColor = ColorUtils.BlendARGB(ThemeHelpers.SwitchOnColor.ToArgb(), Colors.Black.ToPlatform().ToArgb(), 0.5f);
                var unselectedColor = ColorUtils.BlendARGB(ThemeHelpers.SwitchThumbColor.ToArgb(), Colors.Black.ToPlatform().ToArgb(), 0.7f);
                if (ThemeManager.UsingLightTheme)
                {
                    selectedColor = ColorUtils.BlendARGB(ThemeHelpers.SwitchOnColor.ToArgb(), Colors.White.ToPlatform().ToArgb(), 0.7f);
                    unselectedColor = ColorUtils.BlendARGB(ThemeHelpers.SwitchThumbColor.ToArgb(), Colors.Black.ToPlatform().ToArgb(), 0.3f);
                }

                var trackColors = new int[]
                {
                    selectedColor,
                    unselectedColor
                };

                handler.PlatformView.TrackTintList = new ColorStateList(trackStates, trackColors);
            });
        }
    }
}
