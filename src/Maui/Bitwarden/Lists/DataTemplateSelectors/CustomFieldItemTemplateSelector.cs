using Bit.App.Lists.ItemViewModels.CustomFields;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Lists.DataTemplateSelectors
{
    public class CustomFieldItemTemplateSelector : DataTemplateSelector
    {
        public DataTemplate TextTemplate { get; set; }
        public DataTemplate BooleanTemplate { get; set; }
        public DataTemplate LinkedTemplate { get; set; }
        public DataTemplate HiddenTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            switch (item)
            {
                case BooleanCustomFieldItemViewModel _:
                    return BooleanTemplate;
                case LinkedCustomFieldItemViewModel _:
                    return LinkedTemplate;
                case HiddenCustomFieldItemViewModel _:
                    return HiddenTemplate;
                default:
                    return TextTemplate;
            }
        }
    }
}
