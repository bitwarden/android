using System.Collections.Generic;
using System.Linq;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;
using static Android.App.Assist.AssistStructure;
using Android.Text;

namespace Bit.Android.Autofill
{
    public class AutofillFieldMetadata
    {
        private List<string> _autofillHints;
        private string[] _autofillOptions;

        public AutofillFieldMetadata(ViewNode view)
        {
            _autofillOptions = view.GetAutofillOptions();
            Id = view.Id;
            AutofillId = view.AutofillId;
            AutofillType = view.AutofillType;
            InputType = view.InputType;
            IsFocused = view.IsFocused;
            AutofillHints = AutofillHelper.FilterForSupportedHints(view.GetAutofillHints())?.ToList() ?? new List<string>();
        }

        public SaveDataType SaveType { get; set; } = SaveDataType.Generic;
        public List<string> AutofillHints
        {
            get { return _autofillHints; }
            set
            {
                _autofillHints = value;
                UpdateSaveTypeFromHints();
            }
        }
        public int Id { get; private set; }
        public AutofillId AutofillId { get; private set; }
        public AutofillType AutofillType { get; private set; }
        public InputTypes InputType { get; private set; }
        public bool IsFocused { get; private set; }

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
            if(_autofillHints == null)
            {
                return;
            }

            foreach(var hint in _autofillHints)
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