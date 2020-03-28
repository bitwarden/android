using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class MonoEntry : Entry
    {
        public MonoEntry()
        {
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "Menlo-Regular";
                    break;
                case Device.Android:
                    FontFamily = "RobotoMono_Regular.ttf#Roboto Mono";
                    break;
            }
        }
    }
}
