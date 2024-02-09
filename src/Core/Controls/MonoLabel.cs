namespace Bit.App.Controls
{
    public class MonoLabel : Label
    {
        public MonoLabel()
        {
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                FontFamily = "Menlo-Regular";
            }
            else if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                FontFamily = "RobotoMono_Regular.ttf";
            }
        }
    }
}
