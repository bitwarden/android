using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate HeaderTemplate { get; set; }
        public DataTemplate CipherTemplate { get; set; }
        public DataTemplate GroupTemplate { get; set; }
        public DataTemplate LoginCipherTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is GroupingsPageHeaderListItem)
            {
                return HeaderTemplate;
            }

            if (item is GroupingsPageListItem listItem)
            {
                if (listItem.Cipher is null)
                {
                    return GroupTemplate;
                }

                return listItem.Cipher.Type == Core.Enums.CipherType.Login && LoginCipherTemplate != null
                        ? LoginCipherTemplate
                        : CipherTemplate;
            }
            return null;
        }
    }
}
