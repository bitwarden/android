using System.Collections.Generic;
using System.Linq;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;
using static Android.App.Assist.AssistStructure;
using Android.Text;

namespace Bit.Android.Autofill
{
    public class Field
    {
        private List<string> _hints;

        public Field(ViewNode node)
        {
            Id = node.Id;
            IdEntry = node.IdEntry;
            AutofillId = node.AutofillId;
            AutofillType = node.AutofillType;
            InputType = node.InputType;
            Focused = node.IsFocused;
            Selected = node.IsSelected;
            Clickable = node.IsClickable;
            Visible = node.Visibility == ViewStates.Visible;
            Hints = AutofillHelpers.FilterForSupportedHints(node.GetAutofillHints());
            AutofillOptions = node.GetAutofillOptions()?.ToList();
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
        public int Id { get; private set; }
        public string IdEntry { get; set; }
        public AutofillId AutofillId { get; private set; }
        public AutofillType AutofillType { get; private set; }
        public InputTypes InputType { get; private set; }
        public bool Focused { get; private set; }
        public bool Selected { get; private set; }
        public bool Clickable { get; private set; }
        public bool Visible { get; private set; }
        public List<string> AutofillOptions { get; set; }

        public int GetAutofillOptionIndex(string value)
        {
            if(AutofillOptions != null)
            {
                for(var i = 0; i < AutofillOptions.Count; i++)
                {
                    if(AutofillOptions[i].Equals(value))
                    {
                        return i;
                    }
                }
            }

            return -1;
        }

        private void UpdateSaveTypeFromHints()
        {
            SaveType = SaveDataType.Generic;
            if(_hints == null)
            {
                return;
            }

            foreach(var hint in _hints)
            {
                switch(hint)
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