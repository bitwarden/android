namespace Bit.App.Pages
{
    public class SettingsPageHeaderListItem : ISettingsPageListItem
    {
        public SettingsPageHeaderListItem(string title)
        {
            Title = title;
        }

        public string Title { get; }
    }
}
