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
    public class CipherFilledItem : IFilledItem
    {
        private Lazy<string> _password;
        private string _cardNumber;
        private Lazy<string> _cardExpMonth;
        private Lazy<string> _cardExpYear;
        private Lazy<string> _cardCode;

        public CipherFilledItem(Cipher cipher)
        {
            Name = cipher.Name?.Decrypt() ?? "--";
            Type = cipher.Type;

            switch(Type)
            {
                case CipherType.Login:
                    Subtitle = cipher.Login.Username?.Decrypt() ?? string.Empty;
                    Icon = Resource.Drawable.login;
                    _password = new Lazy<string>(() => cipher.Login.Password?.Decrypt());
                    break;
                case CipherType.Card:
                    Subtitle = cipher.Card.Brand?.Decrypt();
                    _cardNumber = cipher.Card.Number?.Decrypt();
                    if(!string.IsNullOrWhiteSpace(_cardNumber) && _cardNumber.Length >= 4)
                    {
                        if(!string.IsNullOrWhiteSpace(_cardNumber))
                        {
                            Subtitle += ", ";
                        }
                        Subtitle += ("*" + _cardNumber.Substring(_cardNumber.Length - 4));
                    }
                    Icon = Resource.Drawable.card;
                    _cardCode = new Lazy<string>(() => cipher.Card.Code?.Decrypt());
                    _cardExpMonth = new Lazy<string>(() => cipher.Card.ExpMonth?.Decrypt());
                    _cardExpYear = new Lazy<string>(() => cipher.Card.ExpYear?.Decrypt());
                    break;
                default:
                    break;
            }
        }

        public CipherFilledItem(VaultListPageModel.Cipher cipher)
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
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(_password.Value));
                    }
                }

                if(fieldCollection.UsernameFields.Any() && !string.IsNullOrWhiteSpace(Subtitle))
                {
                    foreach(var f in fieldCollection.UsernameFields)
                    {
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(Subtitle));
                    }
                }
            }
            else if(Type == CipherType.Card)
            {
                if(fieldCollection.HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardNumber) &&
                    !string.IsNullOrWhiteSpace(_cardNumber))
                {
                    foreach(var f in fieldCollection.HintToFieldsMap[View.AutofillHintCreditCardNumber])
                    {
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(_cardNumber));
                    }
                }
                if(fieldCollection.HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardSecurityCode) &&
                    !string.IsNullOrWhiteSpace(_cardCode.Value))
                {
                    foreach(var f in fieldCollection.HintToFieldsMap[View.AutofillHintCreditCardSecurityCode])
                    {
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(_cardCode.Value));
                    }
                }
                if(fieldCollection.HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardExpirationMonth) &&
                    !string.IsNullOrWhiteSpace(_cardExpMonth.Value))
                {
                    foreach(var f in fieldCollection.HintToFieldsMap[View.AutofillHintCreditCardExpirationMonth])
                    {
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(_cardExpMonth.Value));
                    }
                }
                if(fieldCollection.HintToFieldsMap.ContainsKey(View.AutofillHintCreditCardExpirationYear) &&
                    !string.IsNullOrWhiteSpace(_cardExpYear.Value))
                {
                    foreach(var f in fieldCollection.HintToFieldsMap[View.AutofillHintCreditCardExpirationYear])
                    {
                        setValues = true;
                        datasetBuilder.SetValue(f.AutofillId, AutofillValue.ForText(_cardExpYear.Value));
                    }
                }
            }

            return setValues;
        }
    }
}