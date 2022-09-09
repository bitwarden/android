using Bit.Core.Models.View;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public interface ICustomFieldItemViewModel
    {
        FieldView Field { get; set; }

        bool ShowCopyButton { get; }

        void TriggerFieldChanged();
    }
}
