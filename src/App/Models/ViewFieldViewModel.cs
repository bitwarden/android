using Bit.App.Pages;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Models
{
    public class ViewFieldViewModel : BaseViewModel
    {
        private FieldView _field;
        private bool _showHiddenValue;

        public ViewFieldViewModel(FieldView field)
        {
            Field = field;
            ToggleHiddenValueCommand = new Command(ToggleHiddenValue);
        }

        public FieldView Field
        {
            get => _field;
            set => SetProperty(ref _field, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ValueText),
                    nameof(IsBooleanType),
                    nameof(IsHiddenType),
                    nameof(IsTextType),
                    nameof(ShowCopyButton),
                });
        }

        public bool ShowHiddenValue
        {
            get => _showHiddenValue;
            set => SetProperty(ref _showHiddenValue, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowHiddenValueIcon)
                });
        }

        public Command ToggleHiddenValueCommand { get; set; }

        public string ValueText => IsBooleanType ? (_field.Value == "true" ? "" : "") : _field.Value;
        public string ShowHiddenValueIcon => _showHiddenValue ? "" : "";
        public bool IsTextType => _field.Type == Core.Enums.FieldType.Text;
        public bool IsBooleanType => _field.Type == Core.Enums.FieldType.Boolean;
        public bool IsHiddenType => _field.Type == Core.Enums.FieldType.Hidden;
        public bool ShowCopyButton => _field.Type != Core.Enums.FieldType.Boolean && 
            !string.IsNullOrWhiteSpace(_field.Value);

        public void ToggleHiddenValue()
        {
            ShowHiddenValue = !ShowHiddenValue;
        }
    }
}
