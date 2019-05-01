using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class GroupingsPageListGroup : List<GroupingsPageListItem>
    {
        public GroupingsPageListGroup(string name, int count, bool doUpper = true)
            : this(new List<GroupingsPageListItem>(), name, count, doUpper)
        { }

        public GroupingsPageListGroup(List<GroupingsPageListItem> groupItems, string name, int count,
            bool doUpper = true)
        {
            AddRange(groupItems);
            if(string.IsNullOrWhiteSpace(name))
            {
                Name = "-";
            }
            else if(doUpper)
            {
                Name = name.ToUpperInvariant();
            }
            else
            {
                Name = name;
            }
            ItemCount = count.ToString("N0");
        }

        public string Name { get; set; }
        public string NameShort => string.IsNullOrWhiteSpace(Name) || Name.Length == 0 ? "-" : Name[0].ToString();
        public string ItemCount { get; set; }
    }
}
