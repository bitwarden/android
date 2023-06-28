using System;
using System.Globalization;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Xamarin.Forms;

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
        public string AutomationId
        {
            get
            {
                if (!UseFrame)
                {
                    var idText = new CultureInfo("en-US", false)
                    .TextInfo
                    .ToTitleCase(Name)
                    .Replace(" ", String.Empty)
                    .Replace("-", String.Empty);
                    return $"{idText}Cell";
                }
                else
                {
                    return "EnabledPolicyCell";
                }
            }
        }
    }
}
