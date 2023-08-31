using Bit.App.Effects;

namespace Bit.App.Controls
{
    public class IconLabel : Label
    {
        public bool ShouldUpdateFontSizeDynamicallyForAccesibility { get; set; }

        public IconLabel()
        {
            FontFamily = "bwi-font";
            Effects.Add(new RemoveFontPaddingEffect());
        }
    }
}
