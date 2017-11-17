using System;
using Android.Service.Autofill;
using Android.Views.Autofill;
using System.Linq;
using Android.Text;
using Bit.App.Models;
using Bit.App.Enums;
using Bit.App.Models.Page;

namespace Bit.Android.Autofill
{
    public class CipherFilledItem : IFilledItem
    {
        private Lazy<string> _password;

        public CipherFilledItem(Cipher cipher)
        {
            Name = cipher.Name?.Decrypt() ?? "--";
            Type = cipher.Type;

            switch(Type)
            {
                case CipherType.Login:
                    Subtitle = cipher.Login.Username?.Decrypt() ?? string.Empty;
                    _password = new Lazy<string>(() => cipher.Login.Password?.Decrypt());
                    Icon = Resource.Drawable.login;
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
            if(Type == CipherType.Login)
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

                if(string.IsNullOrWhiteSpace(_password.Value))
                {
                    return false;
                }

                datasetBuilder.SetValue(passwordField.AutofillId, AutofillValue.ForText(_password.Value));

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