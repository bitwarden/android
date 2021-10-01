using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class IconLabel : Label
    {
        public IconLabel()
        {
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "bwi-font";
                    break;
                case Device.Android:
                    FontFamily = "bwi-font.ttf#bwi-font";
                    break;
            }
        }
    }
}
