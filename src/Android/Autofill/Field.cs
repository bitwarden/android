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
        private string[] _autofillOptions;

        public Field(ViewNode view)
        {
            _autofillOptions = view.GetAutofillOptions();
            Id = view.Id;
            AutofillId = view.AutofillId;
            AutofillType = view.AutofillType;
            InputType = view.InputType;
            Focused = view.IsFocused;
            Hints = AutofillHelpers.FilterForSupportedHints(view.GetAutofillHints())?.ToList() ?? new List<string>();
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
        public AutofillId AutofillId { get; private set; }
        public AutofillType AutofillType { get; private set; }
        public InputTypes InputType { get; private set; }
        public bool Focused { get; private set; }

        /**
         * When the {@link ViewNode} is a list that the user needs to choose a string from (i.e. a
         * spinner), this is called to return the index of a specific item in the list.
         */
        public int GetAutofillOptionIndex(string value)
        {
            for(var i = 0; i < _autofillOptions.Length; i++)
            {
                if(_autofillOptions[i].Equals(value))
                {
                    return i;
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