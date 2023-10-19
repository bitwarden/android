namespace Bit.App.Controls
{
    public class MonoLabel : Label
    {
        public MonoLabel()
        {
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                FontFamily = "Menlo-Regular";

                //[MAUI-Migration] Temporary Workaround for the Text to appear in iOS.
                // A proper solution needs to be found to be able to have html text with different colors or alternatively use Label FormattedString Spans
                TextColor = Colors.Black;
            }
            else if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                FontFamily = "RobotoMono_Regular.ttf";
            }
        }
    }
}
