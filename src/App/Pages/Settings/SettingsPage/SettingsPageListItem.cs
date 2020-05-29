using Bit.App.Resources;
using Bit.App.Utilities;
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
            ThemeManager.GetResourceColor("SuccessColor") :
            ThemeManager.GetResourceColor("MutedColor");
    }
}
