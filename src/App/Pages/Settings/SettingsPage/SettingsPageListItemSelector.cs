using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate RegularTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is SettingsPageListItem listItem)
            {
                return RegularTemplate;
            }
            return null;
        }
    }
}
