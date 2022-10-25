using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GroupingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate HeaderTemplate { get; set; }
        public DataTemplate CipherTemplate { get; set; }
        public DataTemplate GroupTemplate { get; set; }
        public DataTemplate SwipeableCipherTemplate { get; set; }
        public DataTemplate AuthenticatorTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is GroupingsPageHeaderListItem)
            {
                return HeaderTemplate;
            }

            if (item is GroupingsPageTOTPListItem)
            {
                return AuthenticatorTemplate;
            }

            if (item is GroupingsPageListItem listItem)
            {
                if (listItem.Cipher is null)
                {
                    return GroupTemplate;
                }

                return CipherTemplate;

                //return listItem.Cipher.Type != Core.Enums.CipherType.Identity && SwipeableCipherTemplate != null
                //        ? SwipeableCipherTemplate
                //        : CipherTemplate;
            }

            return null;
        }
    }
}
