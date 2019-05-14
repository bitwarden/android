using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class SettingsPageListGroup : List<SettingsPageListItem>
    {
        public SettingsPageListGroup(List<SettingsPageListItem> groupItems, string name, bool doUpper = true)
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
    }
}
