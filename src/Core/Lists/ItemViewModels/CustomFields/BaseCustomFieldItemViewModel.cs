using System.Windows.Input;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public abstract class BaseCustomFieldItemViewModel : ExtendedViewModel, ICustomFieldItemViewModel
    {
        protected FieldView _field;
        protected bool _isEditing;
        private string[] _additionalFieldProperties = new string[]
        {
            nameof(ValueText),
            nameof(ShowCopyButton)
        };

        public BaseCustomFieldItemViewModel(FieldView field, bool isEditing, ICommand fieldOptionsCommand)
        {
            _field = field;
            _isEditing = isEditing;
            FieldOptionsCommand = new Command(() => fieldOptionsCommand?.Execute(this));
        }

        public FieldView Field
        {
            get => _field;
            set => SetProperty(ref _field, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ValueText),
                    nameof(ShowCopyButton),
                });
        }

        public bool IsEditing => _isEditing;

        public virtual bool ShowCopyButton => false;

        public virtual string ValueText => _field.Value;

        public ICommand FieldOptionsCommand { get; }

        public void TriggerFieldChanged()
        {
            TriggerPropertyChanged(nameof(Field), _additionalFieldProperties);
        }
    }
}
