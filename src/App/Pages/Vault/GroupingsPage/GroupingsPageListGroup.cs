using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class GroupingsPageListGroup : List<GroupingsPageListItem>
    {
        public GroupingsPageListGroup(string name, bool doUpper = true)
            : this(new List<GroupingsPageListItem>(), name, doUpper)
        { }

        public GroupingsPageListGroup(List<GroupingsPageListItem> groupItems, string name, bool doUpper = true)
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
        }

        public string Name { get; set; }
        public string NameShort => string.IsNullOrWhiteSpace(Name) || Name.Length == 0 ? "-" : Name[0].ToString();
    }
}
