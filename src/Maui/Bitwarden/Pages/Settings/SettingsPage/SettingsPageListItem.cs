using System;
using System.Globalization;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.App.Utilities.Automation;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public class SettingsPageListItem : ISettingsPageListItem
    {
        public string Icon { get; set; }
        public string Name { get; set; }
        public string SubLabel { get; set; }
        public TimeSpan? Time { get; set; }
        public bool UseFrame { get; set; }
        public Func<Task> ExecuteAsync { get; set; }

        public bool SubLabelTextEnabled => SubLabel == AppResources.On;
        public string LineBreakMode => SubLabel == null ? "TailTruncation" : "";
        public bool ShowSubLabel => SubLabel.Length != 0;
        public bool ShowTimeInput => Time != null;
        public Color SubLabelColor => SubLabelTextEnabled ?
            ThemeManager.GetResourceColor("SuccessColor") :
            ThemeManager.GetResourceColor("MutedColor");

        public string AutomationIdSettingName
        {
            get
            {
                return AutomationIdsHelper.AddSuffixFor(
                    UseFrame ? "EnabledPolicy"
                             : AutomationIdsHelper.ToEnglishTitleCase(Name)
                    , SuffixType.Cell);
            }
        }

        public string AutomationIdSettingStatus
        {
            get
            {
                if (UseFrame)
                {
                    return null;
                }

                return AutomationIdsHelper.AddSuffixFor(AutomationIdsHelper.ToEnglishTitleCase(Name), SuffixType.SettingValue);
            }
        }
    }
}
