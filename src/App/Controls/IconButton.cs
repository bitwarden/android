using Bit.App.Effects;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class IconButton : Button
    {
        public IconButton()
        {
            Padding = 0;
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "bwi-font";
                    break;
                case Device.Android:
                    FontFamily = "bwi-font.ttf#bwi-font";
                    break;
            }

            Effects.Add(new RemoveFontPaddingEffect());
        }
    }
}
