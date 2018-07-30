using Bit.App.Models.Api;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models.Data
{
    public class LoginDataModel : CipherDataModel
    {
        private string _uri;

        public LoginDataModel() { }

        public LoginDataModel(CipherResponse response)
            : base(response)
        {
            if(response?.Login == null)
            {
                throw new ArgumentNullException(nameof(response.Login));
            }

            Uris = response.Login.Uris?.Where(u => u != null).Select(u => new LoginUriDataModel(u));
            Username = response.Login.Username;
            Password = response.Login.Password;
            PasswordRevisionDate = response.Login.PasswordRevisionDate;
            Totp = response.Login.Totp;
        }

        public string Uri
        {
            get => Uris?.FirstOrDefault()?.Uri ?? _uri;
            set { _uri = value; }
        }
        public IEnumerable<LoginUriDataModel> Uris { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public DateTime? PasswordRevisionDate { get; set; }
        public string Totp { get; set; }
    }
}
