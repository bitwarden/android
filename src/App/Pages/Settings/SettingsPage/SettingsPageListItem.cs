using Bit.App.Resources;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageListItem
    {
        public string Icon { get; set; }
        public string Name { get; set; }
        public string SubLabel { get; set; }
        public bool SubLabelTextEnabled => SubLabel == AppResources.Enabled;
        public Color SubLabelColor => SubLabelTextEnabled ?
            (Color)Application.Current.Resources["SuccessColor"] :
            (Color)Application.Current.Resources["MutedColor"];
    }
}
