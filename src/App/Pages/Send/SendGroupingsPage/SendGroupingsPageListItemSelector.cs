using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SendGroupingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate SendTemplate { get; set; }
        public DataTemplate GroupTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is SendGroupingsPageListItem listItem)
            {
                return listItem.Send != null ? SendTemplate : GroupTemplate;
            }
            return null;
        }
    }
}
