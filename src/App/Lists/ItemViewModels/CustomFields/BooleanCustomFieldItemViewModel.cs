using System.Windows.Input;
using Bit.Core.Models.View;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public class BooleanCustomFieldItemViewModel : BaseCustomFieldItemViewModel
    {
        public BooleanCustomFieldItemViewModel(FieldView field, bool isEditing, ICommand fieldOptionsCommand)
            : base(field, isEditing, fieldOptionsCommand)
        {
        }

        public bool BooleanValue
        {
            get => bool.TryParse(Field.Value, out var boolVal) && boolVal;
            set
            {
                Field.Value = value.ToString().ToLower();
                TriggerPropertyChanged(nameof(BooleanValue));
            }
        }
    }
}
