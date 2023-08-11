using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate HeaderTemplate { get; set; }
        public DataTemplate RegularTemplate { get; set; }
        public DataTemplate TimePickerTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is SettingsPageHeaderListItem)
            {
                return HeaderTemplate;
            }
            if (item is SettingsPageListItemOld listItem)
            {
                return listItem.ShowTimeInput ? TimePickerTemplate : RegularTemplate;
            }
            return null;
        }
    }
}
