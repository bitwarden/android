using Bit.App.Utilities.Automation;

namespace Bit.App.Pages
{
    public class GroupingsPageHeaderListItem : IGroupingsPageListItem
    {
        public GroupingsPageHeaderListItem(string title, string itemCount)
        {
            Title = title;
            ItemCount = itemCount;
        }

        public string Title { get; }
        public string ItemCount { get; set; }
        public string AutomationId
        {
            get
            {
                return AutomationIdsHelper.AddSuffixFor(AutomationIdsHelper.ToEnglishTitleCase(Title), SuffixType.Header);
            }
        }
    }
}
