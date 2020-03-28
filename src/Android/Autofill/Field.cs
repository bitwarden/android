using System.Collections.Generic;
using System.Linq;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;
using static Android.App.Assist.AssistStructure;
using Android.Text;
using static Android.Views.ViewStructure;

namespace Bit.Droid.Autofill
{
    public class Field
    {
        private List<string> _hints;

        public Field(ViewNode node)
        {
            Id = node.Id;
            TrackingId = $"{node.Id}_{node.GetHashCode()}";
            IdEntry = node.IdEntry;
            AutofillId = node.AutofillId;
            AutofillType = node.AutofillType;
            InputType = node.InputType;
            Focused = node.IsFocused;
            Selected = node.IsSelected;
            Clickable = node.IsClickable;
            Visible = node.Visibility == ViewStates.Visible;
            Hints = FilterForSupportedHints(node.GetAutofillHints());
            Hint = node.Hint;
            AutofillOptions = node.GetAutofillOptions()?.ToList();
            HtmlInfo = node.HtmlInfo;
            Node = node;

            if (node.AutofillValue != null)
            {
                if (node.AutofillValue.IsList)
                {
                    var autofillOptions = node.GetAutofillOptions();
                    if (autofillOptions != null && autofillOptions.Length > 0)
                    {
                        ListValue = node.AutofillValue.ListValue;
                        TextValue = autofillOptions[node.AutofillValue.ListValue];
                    }
                }
                else if (node.AutofillValue.IsDate)
                {
                    DateValue = node.AutofillValue.DateValue;
                }
                else if (node.AutofillValue.IsText)
                {
                    TextValue = node.AutofillValue.TextValue;
                }
                else if (node.AutofillValue.IsToggle)
                {
                    ToggleValue = node.AutofillValue.ToggleValue;
                }
            }
        }

        public SaveDataType SaveType { get; set; } = SaveDataType.Generic;
        public List<string> Hints
        {
            get => _hints;
            set
            {
                _hints = value;
                UpdateSaveTypeFromHints();
            }
        }
        public string Hint { get; set; }
        public int Id { get; private set; }
        public string TrackingId { get; private set; }
        public string IdEntry { get; set; }
        public AutofillId AutofillId { get; private set; }
        public AutofillType AutofillType { get; private set; }
        public InputTypes InputType { get; private set; }
        public bool Focused { get; private set; }
        public bool Selected { get; private set; }
        public bool Clickable { get; private set; }
        public bool Visible { get; private set; }
        public List<string> AutofillOptions { get; set; }
        public string TextValue { get; set; }
        public long? DateValue { get; set; }
        public int? ListValue { get; set; }
        public bool? ToggleValue { get; set; }
        public HtmlInfo HtmlInfo { get; private set; }
        public ViewNode Node { get; private set; }

        public bool ValueIsNull()
        {
            return TextValue == null && DateValue == null && ToggleValue == null;
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null || GetType() != obj.GetType())
            {
                return false;
            }
            var field = obj as Field;
            if (TextValue != null ? !TextValue.Equals(field.TextValue) : field.TextValue != null)
            {
                return false;
            }
            if (DateValue != null ? !DateValue.Equals(field.DateValue) : field.DateValue != null)
            {
                return false;
            }
            return ToggleValue != null ? ToggleValue.Equals(field.ToggleValue) : field.ToggleValue == null;
        }

        public override int GetHashCode()
        {
            var result = TextValue != null ? TextValue.GetHashCode() : 0;
            result = 31 * result + (DateValue != null ? DateValue.GetHashCode() : 0);
            result = 31 * result + (ToggleValue != null ? ToggleValue.GetHashCode() : 0);
            return result;
        }

        private static List<string> FilterForSupportedHints(string[] hints)
        {
            return hints?.Where(h => IsValidHint(h)).ToList() ?? new List<string>();
        }

        private static bool IsValidHint(string hint)
        {
            switch (hint)
            {
                case View.AutofillHintCreditCardExpirationDate:
                case View.AutofillHintCreditCardExpirationDay:
                case View.AutofillHintCreditCardExpirationMonth:
                case View.AutofillHintCreditCardExpirationYear:
                case View.AutofillHintCreditCardNumber:
                case View.AutofillHintCreditCardSecurityCode:
                case View.AutofillHintEmailAddress:
                case View.AutofillHintPhone:
                case View.AutofillHintName:
                case View.AutofillHintPassword:
                case View.AutofillHintPostalAddress:
                case View.AutofillHintPostalCode:
                case View.AutofillHintUsername:
                    return true;
                default:
                    return false;
            }
        }

        private void UpdateSaveTypeFromHints()
        {
            SaveType = SaveDataType.Generic;
            if (_hints == null)
            {
                return;
            }

            foreach (var hint in _hints)
            {
                switch (hint)
                {
                    case View.AutofillHintCreditCardExpirationDate:
                    case View.AutofillHintCreditCardExpirationDay:
                    case View.AutofillHintCreditCardExpirationMonth:
                    case View.AutofillHintCreditCardExpirationYear:
                    case View.AutofillHintCreditCardNumber:
                    case View.AutofillHintCreditCardSecurityCode:
                        SaveType |= SaveDataType.CreditCard;
                        break;
                    case View.AutofillHintEmailAddress:
                        SaveType |= SaveDataType.EmailAddress;
                        break;
                    case View.AutofillHintPhone:
                    case View.AutofillHintName:
                        SaveType |= SaveDataType.Generic;
                        break;
                    case View.AutofillHintPassword:
                        SaveType |= SaveDataType.Password;
                        SaveType &= ~SaveDataType.EmailAddress;
                        SaveType &= ~SaveDataType.Username;
                        break;
                    case View.AutofillHintPostalAddress:
                    case View.AutofillHintPostalCode:
                        SaveType |= SaveDataType.Address;
                        break;
                    case View.AutofillHintUsername:
                        SaveType |= SaveDataType.Username;
                        break;
                }
            }
        }
    }
}
