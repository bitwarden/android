using Bit.App.Effects;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class IconLabel : Label
    {
        public bool ShouldUpdateFontSizeDynamicallyForAccessibility { get; set; }

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

            Effects.Add(new RemoveFontPaddingEffect());
        }
    }
}
