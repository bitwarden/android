using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.App.Utilities.Automation;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageListItem
    {
        private string _nameResourceKey;

        public SettingsPageListItem(string nameResourceKey, Func<Task> executeAsync)
        {
            _nameResourceKey = nameResourceKey;
            ExecuteAsync = executeAsync;
        }

        public string Name => AppResources.ResourceManager.GetString(_nameResourceKey);

        public Func<Task> ExecuteAsync { get; }

        public string AutomationId
        {
            get
            {
                return AutomationIdsHelper.AddSuffixFor(AutomationIdsHelper.ToEnglishTitleCase(_nameResourceKey), SuffixType.Cell);
            }
        }
    }

    [Obsolete("Remove when Settings Reorganization is DONE")]
    public class SettingsPageListItemOld : ISettingsPageListItem
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
