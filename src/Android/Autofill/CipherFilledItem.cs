using System;
using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;
using Bit.App.Models;
using Bit.App.Enums;

namespace Bit.Android.Autofill
{
    public class CipherFilledItem : IFilledItem
    {
        private readonly Cipher _cipher;

        public CipherFilledItem(Cipher cipher)
        {
            _cipher = cipher;
            Name = cipher.Name?.Decrypt() ?? "--";

            switch(cipher.Type)
            {
                case CipherType.Login:
                    Subtitle = _cipher.Login.Username?.Decrypt() ?? string.Empty;
                    break;
                default:
                    break;
            }
        }

        public string Name { get; set; }
        public string Subtitle { get; set; } = string.Empty;

        public bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder)
        {
            if(_cipher.Type == CipherType.Login && _cipher.Login != null)
            {
                var passwordField = fieldCollection.Fields.FirstOrDefault(
                    f => f.InputType.HasFlag(InputTypes.TextVariationPassword));
                if(passwordField == null)
                {
                    passwordField = fieldCollection.Fields.FirstOrDefault(
                        f => f.IdEntry?.ToLower().Contains("password") ?? false);
                }

                if(passwordField == null)
                {
                    return false;
                }

                var password = _cipher.Login.Password?.Decrypt();
                if(string.IsNullOrWhiteSpace(password))
                {
                    return false;
                }

                datasetBuilder.SetValue(passwordField.AutofillId, AutofillValue.ForText(password));

                var usernameField = fieldCollection.Fields.TakeWhile(f => f.Id != passwordField.Id).LastOrDefault();
                if(usernameField != null)
                {
                    if(!string.IsNullOrWhiteSpace(Subtitle))
                    {
                        datasetBuilder.SetValue(usernameField.AutofillId, AutofillValue.ForText(Subtitle));
                    }
                }

                return true;
            }
            else
            {
                return false;
            }
        }
    }
}