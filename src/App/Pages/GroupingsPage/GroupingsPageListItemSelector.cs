using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate CipherTemplate { get; set; }
        public DataTemplate FolderTemplate { get; set; }
        public DataTemplate CollectionTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if(item is GroupingsPageListItem listItem)
            {
                if(listItem.Collection != null)
                {
                    return CollectionTemplate;
                }
                else if(listItem.Folder != null)
                {
                    return FolderTemplate;
                }
                return CipherTemplate;
            }
            return null;
        }
    }
}
