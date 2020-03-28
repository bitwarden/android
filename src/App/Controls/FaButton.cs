using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FaButton : Button
    {
        public FaButton()
        {
            Padding = 0;
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "FontAwesome";
                    break;
                case Device.Android:
                    FontFamily = "FontAwesome.ttf#FontAwesome";
                    break;
            }
        }
    }
}
