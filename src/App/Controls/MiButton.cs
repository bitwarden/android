using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class MiButton : Button
    {
        public MiButton()
        {
            Padding = 0;
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "Material Icons";
                    break;
                case Device.Android:
                    FontFamily = "MaterialIcons_Regular.ttf#Material Icons";
                    break;
            }
        }
    }
}
