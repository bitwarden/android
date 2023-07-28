using System.Windows.Input;
using Bit.Core.Models.View;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public class TextCustomFieldItemViewModel : BaseCustomFieldItemViewModel
    {
        public TextCustomFieldItemViewModel(FieldView field, bool isEditing, ICommand fieldOptionsCommand, ICommand copyFieldCommand)
            : base(field, isEditing, fieldOptionsCommand)
        {
            CopyFieldCommand = new Command(() => copyFieldCommand?.Execute(Field));
        }

        public override bool ShowCopyButton => !_isEditing && !string.IsNullOrWhiteSpace(Field.Value);

        public ICommand CopyFieldCommand { get; }
    }
}
