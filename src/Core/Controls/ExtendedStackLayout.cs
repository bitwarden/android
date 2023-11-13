using CommunityToolkit.Maui.Behaviors;

namespace Bit.App.Controls
{
    public class ExtendedStackLayout : StackLayout
    {
        public ExtendedStackLayout()
        {
            // Add Android Ripple effect. Eventually we should be able to replace this with the Maui Community Toolkit implementation. (https://github.com/CommunityToolkit/Maui/issues/86)
            // [MAUI-Migration] When this TouchBehavior is replaced we can delete the existing TouchBehavior support files (which is all the files and folders inside "Core.Behaviors.PlatformBehaviors.MCTTouch.*")
            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                var touchBehavior = new TouchBehavior()
                {
                    NativeAnimation = true
                };
                Behaviors.Add(touchBehavior);
            }
        }
    }
}
