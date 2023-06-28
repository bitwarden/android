using System.Collections.Generic;
using Bit.App.Utilities.Automation;

namespace Bit.App.Pages
{
    public class GroupingsPageListGroup : List<IGroupingsPageListItem>
    {
        public GroupingsPageListGroup(string name, int count, bool doUpper = true, bool first = false)
            : this(new List<IGroupingsPageListItem>(), name, count, doUpper, first)
        { }

        public GroupingsPageListGroup(IEnumerable<IGroupingsPageListItem> groupItems, string name, int count,
            bool doUpper = true, bool first = false)
        {
            AddRange(groupItems);
            if (string.IsNullOrWhiteSpace(name))
            {
                Name = "-";
            }
            else if (doUpper)
            {
                Name = name.ToUpperInvariant();
            }
            else
            {
                Name = name;
            }
            ItemCount = count.ToString("N0");
            First = first;
        }

        public bool First { get; set; }
        public string Name { get; set; }
        public string NameShort => string.IsNullOrWhiteSpace(Name) || Name.Length == 0 ? "-" : Name[0].ToString();
        public string ItemCount { get; set; }
        public string AutomationId => AutomationIdsHelper.AddSuffixFor(NameShort, SuffixType.ListGroup);
    }
}
