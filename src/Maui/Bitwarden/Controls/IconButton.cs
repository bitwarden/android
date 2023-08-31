using Bit.App.Effects;

namespace Bit.App.Controls
{
    public class IconButton : Button
    {
        public IconButton()
        {
            Padding = 0;
            FontFamily = "bwi-font";
            Effects.Add(new RemoveFontPaddingEffect());
        }
    }
}
