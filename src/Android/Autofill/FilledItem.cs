using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Bit.Core.Enums;
using Android.Views;
using Bit.Core.Models.View;

namespace Bit.Droid.Autofill
{
    public class FilledItem
    {
        private string _password;
        private string _cardName;
        private string _cardNumber;
        private string _cardExpMonth;
        private string _cardExpYear;
        private string _cardCode;
        private string _idPhone;
        private string _idEmail;
        private string _idUsername;
        private string _idAddress;
        private string _idPostalCode;

        public FilledItem(CipherView cipher)
        {
            Id = cipher.Id;
            Name = cipher.Name;
            Type = cipher.Type;
            Subtitle = cipher.SubTitle;

            switch (Type)
            {
                case CipherType.Login:
                    Icon = Resource.Drawable.login;
                    _password = cipher.Login.Password;
                    break;
                case CipherType.Card:
                    _cardNumber = cipher.Card.Number;
                    Icon = Resource.Drawable.card;
                    _cardName = cipher.Card.CardholderName;
                    _cardCode = cipher.Card.Code;
                    _cardExpMonth = cipher.Card.ExpMonth;
                    _cardExpYear = cipher.Card.ExpYear;
                    break;
                case CipherType.Identity:
                    Icon = Resource.Drawable.id;
                    _idPhone = cipher.Identity.Phone;
                    _idEmail = cipher.Identity.Email;
                    _idUsername = cipher.Identity.Username;
                    _idAddress = cipher.Identity.FullAddress;
                    _idPostalCode = cipher.Identity.PostalCode;
                    break;
                default:
                    Icon = Resource.Drawable.login;
                    break;
            }
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Subtitle { get; set; } = string.Empty;
        public int Icon { get; set; } = Resource.Drawable.login;
        public CipherType Type { get; set; }

        public bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder)
        {
            if (!fieldCollection?.Fields.Any() ?? true)
            {
                return false;
            }

            var setValues = false;
            if (Type == CipherType.Login)
            {
if (fieldCollection.UsernameFields.Any() && !string.IsNullOrWhiteSpace(Subtitle))
                {
                    foreach (var f in fieldCollection.UsernameFields)
                    {
                        var val = ApplyValue(f, Subtitle);
                        if (val != null)
                        {
                            setValues = true;
                            datasetBuilder.SetValue(f.AutofillId, val);
                        }
                    }
                }
                if (fieldCollection.PasswordFields.Any() && !string.IsNullOrWhiteSpace(_password))
                {
                    foreach (var f in fieldCollection.PasswordFields)
                    {
                        var val = ApplyValue(f, _password);
                        if (val != null)
                        {
                            setValues = true;
                            datasetBuilder.SetValue(f.AutofillId, val);
                        }
                    }
                }
            }
            else if (Type == CipherType.Card)
            {
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintCreditCardNumber,
                    _cardNumber))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintCreditCardSecurityCode,
                    _cardCode))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection,
                    Android.Views.View.AutofillHintCreditCardExpirationMonth, _cardExpMonth, true))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintCreditCardExpirationYear,
                    _cardExpYear))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintName, _cardName))
                {
                    setValues = true;
                }
            }
            else if (Type == CipherType.Identity)
            {
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintPhone, _idPhone))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintEmailAddress, _idEmail))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintUsername,
                    _idUsername))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintPostalAddress,
                    _idAddress))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintPostalCode,
                    _idPostalCode))
                {
                    setValues = true;
                }
                if (ApplyValue(datasetBuilder, fieldCollection, Android.Views.View.AutofillHintName, Subtitle))
                {
                    setValues = true;
                }
            }
            return setValues;
        }

        private static bool ApplyValue(Dataset.Builder builder, FieldCollection fieldCollection,
            string hint, string value, bool monthValue = false)
        {
            bool setValues = false;
            if (fieldCollection.HintToFieldsMap.ContainsKey(hint) && !string.IsNullOrWhiteSpace(value))
            {
                foreach (var f in fieldCollection.HintToFieldsMap[hint])
                {
                    var val = ApplyValue(f, value, monthValue);
                    if (val != null)
                    {
                        setValues = true;
                        builder.SetValue(f.AutofillId, val);
                    }
                }
            }
            return setValues;
        }

        private static AutofillValue ApplyValue(Field field, string value, bool monthValue = false)
        {
            switch (field.AutofillType)
            {
                case AutofillType.Date:
                    if (long.TryParse(value, out long dateValue))
                    {
                        return AutofillValue.ForDate(dateValue);
                    }
                    break;
                case AutofillType.List:
                    if (field.AutofillOptions != null)
                    {
                        if (monthValue && int.TryParse(value, out int monthIndex))
                        {
                            if (field.AutofillOptions.Count == 13)
                            {
                                return AutofillValue.ForList(monthIndex);
                            }
                            else if (field.AutofillOptions.Count >= monthIndex)
                            {
                                return AutofillValue.ForList(monthIndex - 1);
                            }
                        }
                        for (var i = 0; i < field.AutofillOptions.Count; i++)
                        {
                            if (field.AutofillOptions[i].Equals(value))
                            {
                                return AutofillValue.ForList(i);
                            }
                        }
                    }
                    break;
                case AutofillType.Text:
                    return AutofillValue.ForText(value);
                case AutofillType.Toggle:
                    if (bool.TryParse(value, out bool toggleValue))
                    {
                        return AutofillValue.ForToggle(toggleValue);
                    }
                    break;
                default:
                    break;
            }
            return null;
        }
    }
}
