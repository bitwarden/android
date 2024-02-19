namespace Bit.App.Pages
{
    public class SendGroupingsPageHeaderListItem : ISendGroupingsPageListItem
    {
        public SendGroupingsPageHeaderListItem(string title, string itemCount)
        {
            Title = title;
            ItemCount = itemCount;
        }

        public string Title { get; }
        public string ItemCount { get; }
    }
}
