using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class IconFontImageSource : FontImageSource
    {
        public IconFontImageSource()
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
