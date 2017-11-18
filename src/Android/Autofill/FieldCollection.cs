using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;

namespace Bit.Android.Autofill
{
    public class FieldCollection
    {
        private List<Field> _passwordFields = null;
        private List<Field> _usernameFields = null;

        public HashSet<int> Ids { get; private set; } = new HashSet<int>();
        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType { get; private set; } = SaveDataType.Generic;
        public List<string> Hints { get; private set; } = new List<string>();
        public List<string> FocusedHints { get; private set; } = new List<string>();
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

                _passwordFields = Fields.Where(f => f.InputType.HasFlag(InputTypes.TextVariationPassword)).ToList();
                if(!_passwordFields.Any())
                {
                    _passwordFields = Fields.Where(f => f.IdEntry?.ToLower().Contains("password") ?? false).ToList();
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
                foreach(var passwordField in PasswordFields)
                {
                    var usernameField = Fields.TakeWhile(f => f.Id != passwordField.Id).LastOrDefault();
                    if(usernameField != null)
                    {
                        _usernameFields.Add(usernameField);
                    }
                }
                return _usernameFields;
            }
        }

        public bool FillableForLogin => UsernameFields.Any(f => f.Focused) || PasswordFields.Any(f => f.Focused);

        public void Add(Field field)
        {
            if(Ids.Contains(field.Id))
            {
                return;
            }

            _passwordFields = _usernameFields = null;

            Ids.Add(field.Id);
            Fields.Add(field);
            SaveType |= field.SaveType;
            AutofillIds.Add(field.AutofillId);
            IdToFieldMap.Add(field.Id, field);

            if((field.Hints?.Count ?? 0) > 0)
            {
                Hints.AddRange(field.Hints);
                if(field.Focused)
                {
                    FocusedHints.AddRange(field.Hints);
                }

                foreach(var hint in field.Hints)
                {
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
            if(!Fields?.Any() ?? true)
            {
                return null;
            }

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
    }
}