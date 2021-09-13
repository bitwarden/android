using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FaLabel : Label
    {
        public FaLabel()
        {
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "FontAwesome";
                    break;
                case Device.Android:
                    FontFamily = "Bitwarden_icon_font_v1.ttf#Bitwarden_icon_font_v1";
                    break;
            }
        }
    }
}
