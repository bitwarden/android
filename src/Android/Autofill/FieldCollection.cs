using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;
using Android.Views;

namespace Bit.Droid.Autofill
{
    public class FieldCollection
    {
        private List<Field> _passwordFields = null;
        private List<Field> _usernameFields = null;
        private HashSet<string> _ignoreSearchTerms = new HashSet<string> { "search", "find", "recipient", "edit" };
        private HashSet<string> _usernameTerms = new HashSet<string> { "email", "phone", "username" };
        private HashSet<string> _passwordTerms = new HashSet<string> { "password", "pswd" };

        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType
        {
            get
            {
                if (FillableForLogin)
                {
                    return SaveDataType.Password;
                }
                else if (FillableForCard)
                {
                    return SaveDataType.CreditCard;
                }

                return SaveDataType.Generic;
            }
        }
        public HashSet<string> Hints { get; private set; } = new HashSet<string>();
        public HashSet<string> FocusedHints { get; private set; } = new HashSet<string>();
        public HashSet<string> FieldTrackingIds { get; private set; } = new HashSet<string>();
        public List<Field> Fields { get; private set; } = new List<Field>();
        public IDictionary<string, List<Field>> HintToFieldsMap { get; private set; } =
            new Dictionary<string, List<Field>>();
        public List<AutofillId> IgnoreAutofillIds { get; private set; } = new List<AutofillId>();

        public List<Field> PasswordFields
        {
            get
            {
                if (_passwordFields != null)
                {
                    return _passwordFields;
                }
                if (Hints.Any())
                {
                    _passwordFields = new List<Field>();
                    if (HintToFieldsMap.ContainsKey(View.AutofillHintPassword))
                    {
                        _passwordFields.AddRange(HintToFieldsMap[View.AutofillHintPassword]);
                        return _passwordFields;
                    }
                }

                _passwordFields = Fields.Where(f => FieldIsPassword(f)).ToList();
                if (!_passwordFields.Any())
                {
                    _passwordFields = Fields.Where(f => FieldHasPasswordTerms(f)).ToList();
                }
                return _passwordFields;
            }
        }

        public List<Field> UsernameFields
        {
            get
            {
                if (_usernameFields != null)
                {
                    return _usernameFields;
                }
                _usernameFields = new List<Field>();
                if (Hints.Any())
                {
                    if (HintToFieldsMap.ContainsKey(View.AutofillHintEmailAddress))
                    {
                        _usernameFields.AddRange(HintToFieldsMap[View.AutofillHintEmailAddress]);
                    }
                    if (HintToFieldsMap.ContainsKey(View.AutofillHintUsername))
                    {
                        _usernameFields.AddRange(HintToFieldsMap[View.AutofillHintUsername]);
                    }
                    if (_usernameFields.Any())
                    {
                        return _usernameFields;
                    }
                }

                foreach (var passwordField in PasswordFields)
                {
                    var usernameField = Fields.TakeWhile(f => f.AutofillId != passwordField.AutofillId)
                        .LastOrDefault();
                    if (usernameField != null)
                    {
                        _usernameFields.Add(usernameField);
                    }
                }

                if (!_usernameFields.Any())
                {
                    _usernameFields = Fields.Where(f => FieldIsUsername(f)).ToList();
                }
                return _usernameFields;
            }
        }

        public bool FillableForLogin => FocusedHintsContain(new string[] {
            View.AutofillHintUsername,
            View.AutofillHintEmailAddress,
            View.AutofillHintPassword
        }) || UsernameFields.Any(f => f.Focused) || PasswordFields.Any(f => f.Focused);

        public bool FillableForCard => FocusedHintsContain(new string[] {
            View.AutofillHintCreditCardNumber,
            View.AutofillHintCreditCardExpirationMonth,
            View.AutofillHintCreditCardExpirationYear,
            View.AutofillHintCreditCardSecurityCode
        });

        public bool FillableForIdentity => FocusedHintsContain(new string[] {
            View.AutofillHintName,
            View.AutofillHintPhone,
            View.AutofillHintPostalAddress,
            View.AutofillHintPostalCode
        });

        public bool Fillable => FillableForLogin || FillableForCard || FillableForIdentity;

        public void Add(Field field)
        {
            if (field == null || FieldTrackingIds.Contains(field.TrackingId))
            {
                return;
            }

            _passwordFields = _usernameFields = null;
            FieldTrackingIds.Add(field.TrackingId);
            Fields.Add(field);
            AutofillIds.Add(field.AutofillId);

            if (field.Hints != null)
            {
                foreach (var hint in field.Hints)
                {
                    Hints.Add(hint);
                    if (field.Focused)
                    {
                        FocusedHints.Add(hint);
                    }
                    if (!HintToFieldsMap.ContainsKey(hint))
                    {
                        HintToFieldsMap.Add(hint, new List<Field>());
                    }
                    HintToFieldsMap[hint].Add(field);
                }
            }
        }

        public SavedItem GetSavedItem()
        {
            if (SaveType == SaveDataType.Password)
            {
                var passwordField = PasswordFields.FirstOrDefault(f => !string.IsNullOrWhiteSpace(f.TextValue));
                if (passwordField == null)
                {
                    return null;
                }

                var savedItem = new SavedItem
                {
                    Type = Core.Enums.CipherType.Login,
                    Login = new SavedItem.LoginItem
                    {
                        Password = GetFieldValue(passwordField)
                    }
                };

                var usernameField = Fields.TakeWhile(f => f.AutofillId != passwordField.AutofillId).LastOrDefault();
                savedItem.Login.Username = GetFieldValue(usernameField);
                return savedItem;
            }
            else if (SaveType == SaveDataType.CreditCard)
            {
                var savedItem = new SavedItem
                {
                    Type = Core.Enums.CipherType.Card,
                    Card = new SavedItem.CardItem
                    {
                        Number = GetFieldValue(View.AutofillHintCreditCardNumber),
                        Name = GetFieldValue(View.AutofillHintName),
                        ExpMonth = GetFieldValue(View.AutofillHintCreditCardExpirationMonth, true),
                        ExpYear = GetFieldValue(View.AutofillHintCreditCardExpirationYear),
                        Code = GetFieldValue(View.AutofillHintCreditCardSecurityCode)
                    }
                };
                return savedItem;
            }
            return null;
        }

        public AutofillId[] GetOptionalSaveIds()
        {
            if (SaveType == SaveDataType.Password)
            {
                return UsernameFields.Select(f => f.AutofillId).ToArray();
            }
            else if (SaveType == SaveDataType.CreditCard)
            {
                var fieldList = new List<Field>();
                if (HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardSecurityCode))
                {
                    fieldList.AddRange(HintToFieldsMap[View.AutofillHintCreditCardSecurityCode]);
                }
                if (HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardExpirationYear))
                {
                    fieldList.AddRange(HintToFieldsMap[View.AutofillHintCreditCardExpirationYear]);
                }
                if (HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardExpirationMonth))
                {
                    fieldList.AddRange(HintToFieldsMap[View.AutofillHintCreditCardExpirationMonth]);
                }
                if (HintToFieldsMap.ContainsKey(View.AutofillHintName))
                {
                    fieldList.AddRange(HintToFieldsMap[View.AutofillHintName]);
                }
                return fieldList.Select(f => f.AutofillId).ToArray();
            }
            return new AutofillId[0];
        }

        public AutofillId[] GetRequiredSaveFields()
        {
            if (SaveType == SaveDataType.Password)
            {
                return PasswordFields.Select(f => f.AutofillId).ToArray();
            }
            else if (SaveType == SaveDataType.CreditCard && HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardNumber))
            {
                return HintToFieldsMap[View.AutofillHintCreditCardNumber].Select(f => f.AutofillId).ToArray();
            }
            return new AutofillId[0];
        }

        private bool FocusedHintsContain(IEnumerable<string> hints)
        {
            return hints.Any(h => FocusedHints.Contains(h));
        }

        private string GetFieldValue(string hint, bool monthValue = false)
        {
            if (HintToFieldsMap.ContainsKey(hint))
            {
                foreach (var field in HintToFieldsMap[hint])
                {
                    var val = GetFieldValue(field, monthValue);
                    if (!string.IsNullOrWhiteSpace(val))
                    {
                        return val;
                    }
                }
            }
            return null;
        }

        private string GetFieldValue(Field field, bool monthValue = false)
        {
            if (field == null)
            {
                return null;
            }
            if (!string.IsNullOrWhiteSpace(field.TextValue))
            {
                if (field.AutofillType == AutofillType.List && field.ListValue.HasValue && monthValue)
                {
                    if (field.AutofillOptions.Count == 13)
                    {
                        return field.ListValue.ToString();
                    }
                    else if (field.AutofillOptions.Count == 12)
                    {
                        return (field.ListValue + 1).ToString();
                    }
                }
                return field.TextValue;
            }
            else if (field.DateValue.HasValue)
            {
                return field.DateValue.Value.ToString();
            }
            else if (field.ToggleValue.HasValue)
            {
                return field.ToggleValue.Value.ToString();
            }
            return null;
        }

        private bool FieldIsPassword(Field f)
        {
            var inputTypePassword = f.InputType.HasFlag(InputTypes.TextVariationPassword) ||
                f.InputType.HasFlag(InputTypes.TextVariationVisiblePassword) ||
                f.InputType.HasFlag(InputTypes.TextVariationWebPassword);

            // For whatever reason, multi-line input types are coming through with TextVariationPassword flags
            if (inputTypePassword && f.InputType.HasFlag(InputTypes.TextVariationPassword) &&
                f.InputType.HasFlag(InputTypes.TextFlagMultiLine))
            {
                inputTypePassword = false;
            }

            if (!inputTypePassword && f.HtmlInfo != null && f.HtmlInfo.Tag == "input" &&
                (f.HtmlInfo.Attributes?.Any() ?? false))
            {
                foreach (var a in f.HtmlInfo.Attributes)
                {
                    var key = a.First as Java.Lang.String;
                    var val = a.Second as Java.Lang.String;
                    if (key != null && val != null && key.ToString() == "type" && val.ToString() == "password")
                    {
                        return true;
                    }
                }
            }

            return inputTypePassword && !ValueContainsAnyTerms(f.IdEntry, _ignoreSearchTerms) &&
                !ValueContainsAnyTerms(f.Hint, _ignoreSearchTerms) && !FieldIsUsername(f);
        }

        private bool FieldHasPasswordTerms(Field f)
        {
            return ValueContainsAnyTerms(f.IdEntry, _passwordTerms) || ValueContainsAnyTerms(f.Hint, _passwordTerms);
        }
        
        private bool FieldIsUsername(Field f)
        {
            return f.InputType.HasFlag(InputTypes.TextVariationWebEmailAddress) || FieldHasUsernameTerms(f);
        }

        private bool FieldHasUsernameTerms(Field f)
        {
            return ValueContainsAnyTerms(f.IdEntry, _usernameTerms) || ValueContainsAnyTerms(f.Hint, _usernameTerms);
        }

        private bool ValueContainsAnyTerms(string value, HashSet<string> terms)
        {
            if (string.IsNullOrWhiteSpace(value))
            {
                return false;
            }
            var lowerValue = value.ToLowerInvariant();
            return terms.Any(t => lowerValue.Contains(t));
        }
    }
}
