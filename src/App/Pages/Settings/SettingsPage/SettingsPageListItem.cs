using System;
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
        public TimeSpan? Time { get; set; }
        public bool UseFrame { get; set; }
        public bool SubLabelTextEnabled => SubLabel == AppResources.Enabled;
        public string LineBreakMode => SubLabel == null ? "TailTruncation" : "";
        public bool ShowSubLabel => SubLabel.Length != 0;
        public bool ShowTimeInput => Time != null;
        public Color SubLabelColor => SubLabelTextEnabled ?
            ThemeManager.GetResourceColor("SuccessColor") :
            ThemeManager.GetResourceColor("MutedColor");
    }
}
