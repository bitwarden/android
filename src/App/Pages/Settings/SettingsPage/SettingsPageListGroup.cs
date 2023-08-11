using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class SettingsPageListGroup : List<SettingsPageListItemOld>
    {
        public SettingsPageListGroup(List<SettingsPageListItemOld> groupItems, string name, bool doUpper = true,
            bool first = false)
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
            First = first;
        }

        public bool First { get; set; }
        public string Name { get; set; }
    }
}
