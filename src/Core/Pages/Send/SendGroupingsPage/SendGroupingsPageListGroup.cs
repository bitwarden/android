using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class SendGroupingsPageListGroup : List<SendGroupingsPageListItem>
    {
        public SendGroupingsPageListGroup(string name, int count, bool doUpper = true, bool first = false)
            : this(new List<SendGroupingsPageListItem>(), name, count, doUpper, first) { }

        public SendGroupingsPageListGroup(List<SendGroupingsPageListItem> sendGroupItems, string name, int count,
            bool doUpper = true, bool first = false)
        {
            AddRange(sendGroupItems);
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
            ItemCount = count > 0 ? count.ToString("N0") : "";
            First = first;
        }

        public bool First { get; set; }
        public string Name { get; set; }
        public string NameShort => string.IsNullOrWhiteSpace(Name) || Name.Length == 0 ? "-" : Name[0].ToString();
        public string ItemCount { get; set; }
    }
}
