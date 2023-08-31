using Android.Content.Res;
using Android.Graphics.Drawables;
using Android.OS;
using AndroidX.Core.Content.Resources;
using Bit.App.Droid.Utilities;

namespace Bit.App.Handlers
{
    public partial class SwitchHandlerMappings
    {
        partial void SetupPlatform()
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
        }
    }
}
