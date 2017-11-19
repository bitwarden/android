using System;
using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Bit.App.Models;
using Bit.App.Enums;
using Bit.App.Models.Page;
using Android.Views;

namespace Bit.Android.Autofill
{
    public class FilledItem
    {
        private Lazy<string> _password;
        private Lazy<string> _cardName;
        private string _cardNumber;
        private Lazy<string> _cardExpMonth;
        private Lazy<string> _cardExpYear;
        private Lazy<string> _cardCode;
        private Lazy<string> _idPhone;
        private Lazy<string> _idEmail;
        private Lazy<string> _idUsername;
        private Lazy<string> _idAddress;
        private Lazy<string> _idPostalCode;

        public FilledItem(Cipher cipher)
        {
            Name = cipher.Name?.Decrypt(cipher.OrganizationId) ?? "--";
            Type = cipher.Type;

            switch(Type)
            {
                case CipherType.Login:
                    Subtitle = cipher.Login.Username?.Decrypt(cipher.OrganizationId) ?? string.Empty;
                    Icon = Resource.Drawable.login;
                    _password = new Lazy<string>(() => cipher.Login.Password?.Decrypt(cipher.OrganizationId));
                    break;
                case CipherType.Card:
                    Subtitle = cipher.Card.Brand?.Decrypt(cipher.OrganizationId);
                    _cardNumber = cipher.Card.Number?.Decrypt(cipher.OrganizationId);
                    if(!string.IsNullOrWhiteSpace(_cardNumber) && _cardNumber.Length >= 4)
                    {
                        if(!string.IsNullOrWhiteSpace(_cardNumber))
                        {
                            Subtitle += ", ";
                        }
                        Subtitle += ("*" + _cardNumber.Substring(_cardNumber.Length - 4));
                    }
                    Icon = Resource.Drawable.card;
                    _cardName = new Lazy<string>(() => cipher.Card.CardholderName?.Decrypt(cipher.OrganizationId));
                    _cardCode = new Lazy<string>(() => cipher.Card.Code?.Decrypt(cipher.OrganizationId));
                    _cardExpMonth = new Lazy<string>(() => cipher.Card.ExpMonth?.Decrypt(cipher.OrganizationId));
                    _cardExpYear = new Lazy<string>(() => cipher.Card.ExpYear?.Decrypt(cipher.OrganizationId));
                    break;
                case CipherType.Identity:
                    var firstName = cipher.Identity?.FirstName?.Decrypt(cipher.OrganizationId) ?? " ";
                    var lastName = cipher.Identity?.LastName?.Decrypt(cipher.OrganizationId) ?? " ";
                    Subtitle = " ";
                    if(!string.IsNullOrWhiteSpace(firstName))
                    {
                        Subtitle = firstName;
                    }
                    if(!string.IsNullOrWhiteSpace(lastName))
                    {
                        if(!string.IsNullOrWhiteSpace(Subtitle))
                        {
                            Subtitle += " ";
                        }
                        Subtitle += lastName;
                    }
                    Icon = Resource.Drawable.id;
                    _idPhone = new Lazy<string>(() => cipher.Identity.Phone?.Decrypt(cipher.OrganizationId));
                    _idEmail = new Lazy<string>(() => cipher.Identity.Email?.Decrypt(cipher.OrganizationId));
                    _idUsername = new Lazy<string>(() => cipher.Identity.Username?.Decrypt(cipher.OrganizationId));
                    _idAddress = new Lazy<string>(() =>
                    {
                        var address = cipher.Identity.Address1?.Decrypt(cipher.OrganizationId);

                        var address2 = cipher.Identity.Address2?.Decrypt(cipher.OrganizationId);
                        if(!string.IsNullOrWhiteSpace(address2))
                        {
                            if(!string.IsNullOrWhiteSpace(address))
                            {
                                address += ", ";
                            }

                            address += address2;
                        }

                        var address3 = cipher.Identity.Address3?.Decrypt(cipher.OrganizationId);
                        if(!string.IsNullOrWhiteSpace(address3))
                        {
                            if(!string.IsNullOrWhiteSpace(address))
                            {
                                address += ", ";
                            }

                            address += address3;
                        }

                        return address;
                    });
                    _idPostalCode = new Lazy<string>(() => cipher.Identity.PostalCode?.Decrypt(cipher.OrganizationId));
                    break;
                default:
                    break;
            }
        }

        public FilledItem(VaultListPageModel.Cipher cipher)
        {
            Name = cipher.Name ?? "--";
            Type = cipher.Type;

            switch(Type)
            {
                case CipherType.Login:
                    Subtitle = cipher.LoginUsername ?? string.Empty;
                    _password = cipher.LoginPassword;
                    Icon = Resource.Drawable.login;
                    break;
                default:
                    break;
            }
        }

        public string Name { get; set; }
        public string Subtitle { get; set; } = string.Empty;
        public int Icon { get; set; } = Resource.Drawable.login;
        public CipherType Type { get; set; }

        public bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder)
        {
            if(!fieldCollection?.Fields.Any() ?? true)
            {
                return false;
            }

            var setValues = false;
            if(Type == CipherType.Login)
            {
                if(fieldCollection.PasswordFields.Any() && !string.IsNullOrWhiteSpace(_password.Value))
                {
                    foreach(var f in fieldCollection.PasswordFields)
                    {
                        var val = ApplyValue(f, _password.Value);
                        if(val != null)
                        {
                            setValues = true;
                            datasetBuilder.SetValue(f.AutofillId, val);
                        }
                    }
                }

                if(fieldCollection.UsernameFields.Any() && !string.IsNullOrWhiteSpace(Subtitle))
                {
                    foreach(var f in fieldCollection.UsernameFields)
                    {
                        var val = ApplyValue(f, Subtitle);
                        if(val != null)
                        {
                            setValues = true;
                            datasetBuilder.SetValue(f.AutofillId, val);
                        }
                    }
                }
            }
            else if(Type == CipherType.Card)
            {
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintCreditCardNumber,
                    new Lazy<string>(() => _cardNumber)))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintCreditCardSecurityCode, _cardCode))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintCreditCardExpirationMonth, _cardExpMonth, true))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintCreditCardExpirationYear, _cardExpYear))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintName, _cardName))
                {
                    setValues = true;
                }
            }
            else if(Type == CipherType.Identity)
            {
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintPhone, _idPhone))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintEmailAddress, _idEmail))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintUsername, _idUsername))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintPostalAddress, _idAddress))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintPostalCode, _idPostalCode))
                {
                    setValues = true;
                }
                if(ApplyValue(datasetBuilder, fieldCollection, View.AutofillHintName, new Lazy<string>(() => Subtitle)))
                {
                    setValues = true;
                }
            }

            return setValues;
        }

        private static bool ApplyValue(Dataset.Builder builder, FieldCollection fieldCollection,
            string hint, Lazy<string> value, bool monthValue = false)
        {
            bool setValues = false;
            if(fieldCollection.HintToFieldsMap.ContainsKey(hint) && !string.IsNullOrWhiteSpace(value.Value))
            {
                foreach(var f in fieldCollection.HintToFieldsMap[hint])
                {
                    var val = ApplyValue(f, value.Value, monthValue);
                    if(val != null)
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
            switch(field.AutofillType)
            {
                case AutofillType.Date:
                    if(long.TryParse(value, out long dateValue))
                    {
                        return AutofillValue.ForDate(dateValue);
                    }
                    break;
                case AutofillType.List:
                    if(field.AutofillOptions != null)
                    {
                        if(monthValue && int.TryParse(value, out int monthIndex))
                        {
                            if(field.AutofillOptions.Count == 13)
                            {
                                return AutofillValue.ForList(monthIndex);
                            }
                            else if(field.AutofillOptions.Count >= monthIndex)
                            {
                                return AutofillValue.ForList(monthIndex - 1);
                            }
                        }

                        for(var i = 0; i < field.AutofillOptions.Count; i++)
                        {
                            if(field.AutofillOptions[i].Equals(value))
                            {
                                return AutofillValue.ForList(i);
                            }
                        }
                    }
                    break;
                case AutofillType.Text:
                    return AutofillValue.ForText(value);
                case AutofillType.Toggle:
                    if(bool.TryParse(value, out bool toggleValue))
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