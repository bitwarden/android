using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class MonoLabel : Label
    {
        public MonoLabel()
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
