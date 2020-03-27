using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class MiLabel : Label
    {
        public MiLabel()
        {
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
