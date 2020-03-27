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
                    FontFamily = "FontAwesome.ttf#FontAwesome";
                    break;
            }
        }
    }
}
