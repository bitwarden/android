using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;
using Android.Views;

namespace Bit.Android.Autofill
{
    public class FieldCollection
    {
        private List<Field> _passwordFields = null;
        private List<Field> _usernameFields = null;

        public HashSet<int> Ids { get; private set; } = new HashSet<int>();
        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType
        {
            get
            {
                if(FillableForLogin)
                {
                    return SaveDataType.Password;
                }
                else if(FillableForCard)
                {
                    return SaveDataType.CreditCard;
                }

                return SaveDataType.Generic;
            }
        }
        public HashSet<string> Hints { get; private set; } = new HashSet<string>();
        public HashSet<string> FocusedHints { get; private set; } = new HashSet<string>();
        public List<Field> Fields { get; private set; } = new List<Field>();
        public IDictionary<int, Field> IdToFieldMap { get; private set; } =
            new Dictionary<int, Field>();
        public IDictionary<string, List<Field>> HintToFieldsMap { get; private set; } =
            new Dictionary<string, List<Field>>();
        public List<AutofillId> IgnoreAutofillIds { get; private set; } = new List<AutofillId>();

        public List<Field> PasswordFields
        {
            get
            {
                if(_passwordFields != null)
                {
                    return _passwordFields;
                }

                if(Hints.Any())
                {
                    _passwordFields = new List<Field>();
                    if(HintToFieldsMap.ContainsKey(View.AutofillHintPassword))
                    {
                        _passwordFields.AddRange(HintToFieldsMap[View.AutofillHintPassword]);
                    }
                }
                else
                {
                    _passwordFields = Fields.Where(f => f.InputType.HasFlag(InputTypes.TextVariationPassword)).ToList();
                    if(!_passwordFields.Any())
                    {
                        _passwordFields = Fields.Where(f => f.IdEntry?.ToLower().Contains("password") ?? false).ToList();
                    }
                }

                return _passwordFields;
            }
        }

        public List<Field> UsernameFields
        {
            get
            {
                if(_usernameFields != null)
                {
                    return _usernameFields;
                }

                _usernameFields = new List<Field>();
                if(Hints.Any())
                {
                    if(HintToFieldsMap.ContainsKey(View.AutofillHintEmailAddress))
                    {
                        _usernameFields.AddRange(HintToFieldsMap[View.AutofillHintEmailAddress]);
                    }
                    if(HintToFieldsMap.ContainsKey(View.AutofillHintUsername))
                    {
                        _usernameFields.AddRange(HintToFieldsMap[View.AutofillHintUsername]);
                    }
                }
                else
                {
                    foreach(var passwordField in PasswordFields)
                    {
                        var usernameField = Fields.TakeWhile(f => f.Id != passwordField.Id).LastOrDefault();
                        if(usernameField != null)
                        {
                            _usernameFields.Add(usernameField);
                        }
                    }
                }

                return _usernameFields;
            }
        }

        public bool FillableForLogin => FocusedHintsContain(
            new string[] { View.AutofillHintUsername, View.AutofillHintEmailAddress, View.AutofillHintPassword }) ||
            UsernameFields.Any(f => f.Focused) || PasswordFields.Any(f => f.Focused);
        public bool FillableForCard => FocusedHintsContain(
            new string[] { View.AutofillHintCreditCardNumber, View.AutofillHintCreditCardExpirationMonth,
                View.AutofillHintCreditCardExpirationYear, View.AutofillHintCreditCardSecurityCode});
        public bool FillableForIdentity => FocusedHintsContain(
            new string[] { View.AutofillHintName, View.AutofillHintPhone, View.AutofillHintPostalAddress,
                View.AutofillHintPostalCode });

        public void Add(Field field)
        {
            if(Ids.Contains(field.Id))
            {
                return;
            }

            _passwordFields = _usernameFields = null;

            Ids.Add(field.Id);
            Fields.Add(field);
            AutofillIds.Add(field.AutofillId);
            IdToFieldMap.Add(field.Id, field);

            if(field.Hints != null)
            {
                foreach(var hint in field.Hints)
                {
                    Hints.Add(hint);
                    if(field.Focused)
                    {
                        FocusedHints.Add(hint);
                    }

                    if(!HintToFieldsMap.ContainsKey(hint))
                    {
                        HintToFieldsMap.Add(hint, new List<Field>());
                    }

                    HintToFieldsMap[hint].Add(field);
                }
            }
        }

        public SavedItem GetSavedItem()
        {
            if(SaveType == SaveDataType.Password)
            {
                var passwordField = PasswordFields.FirstOrDefault(f => !string.IsNullOrWhiteSpace(f.TextValue));
                if(passwordField == null)
                {
                    return null;
                }

                var savedItem = new SavedItem
                {
                    Type = App.Enums.CipherType.Login,
                    Login = new SavedItem.LoginItem
                    {
                        Password = passwordField.TextValue
                    }
                };

                var usernameField = Fields.TakeWhile(f => f.Id != passwordField.Id).LastOrDefault();
                if(usernameField != null && !string.IsNullOrWhiteSpace(usernameField.TextValue))
                {
                    savedItem.Login.Username = usernameField.TextValue;
                }

                return savedItem;
            }
            else if(SaveType == SaveDataType.CreditCard)
            {
                var savedItem = new SavedItem
                {
                    Type = App.Enums.CipherType.Card,
                    Card = new SavedItem.CardItem
                    {
                        Number = HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardNumber) ?
                            HintToFieldsMap[View.AutofillHintCreditCardNumber].FirstOrDefault(
                                f => !string.IsNullOrWhiteSpace(f.TextValue))?.TextValue : null
                    }
                };

                return savedItem;
            }

            return null;
        }

        private bool FocusedHintsContain(IEnumerable<string> hints)
        {
            return hints.Any(h => FocusedHints.Contains(h));
        }
    }
}