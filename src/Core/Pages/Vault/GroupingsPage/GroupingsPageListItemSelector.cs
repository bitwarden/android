namespace Bit.App.Pages
{
    public class GroupingsPageListItemSelector : DataTemplateSelector
    {
        public DataTemplate HeaderTemplate { get; set; }
        public DataTemplate CipherTemplate { get; set; }
        public DataTemplate GroupTemplate { get; set; }
        public DataTemplate AuthenticatorTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if (item is GroupingsPageTOTPListItem)
            {
                return AuthenticatorTemplate;
            }

            if (item is CipherItemViewModel)
            {
                return CipherTemplate;
            }

            if (item is GroupingsPageHeaderListItem)
            {
                return HeaderTemplate;
            }

            if (item is GroupingsPageListItem)
            {
                return GroupTemplate;
            }

            return null;
        }
    }
}
