using CommunityToolkit.Maui.Behaviors;

namespace Bit.App.Controls
{
    public class ExtendedGrid : Grid
    {
        public ExtendedGrid()
        {
#if ANDROID
            // Add Android Ripple effect. Eventually we should be able to replace this with the Maui Community Toolkit implementation. (https://github.com/CommunityToolkit/Maui/issues/86)
            // TODO: [TouchEffect] When this TouchBehavior is replaced we can delete the existing TouchBehavior support files (which is all the files and folders inside "Core.Behaviors.PlatformBehaviors.MCTTouch.*")
            var touchBehavior = new TouchBehavior()
            {
                NativeAnimation = true,
                ShouldMakeChildrenInputTransparent = false
            };
            Behaviors.Add(touchBehavior);
#endif
        }
    }
}
