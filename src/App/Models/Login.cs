using Bit.App.Models.Data;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models
{
    public class Login
    {
        public Login() { }

        public Login(CipherData data)
        {
            LoginDataModel deserializedData;
            if(data.Login != null)
            {
                deserializedData = JsonConvert.DeserializeObject<LoginDataModel>(data.Login);
            }
            else if(data.Data != null)
            {
                deserializedData = JsonConvert.DeserializeObject<LoginDataModel>(data.Data);
            }
            else
            {
                throw new ArgumentNullException(nameof(data.Identity));
            }

            Username = deserializedData.Username != null ? new CipherString(deserializedData.Username) : null;
            Password = deserializedData.Password != null ? new CipherString(deserializedData.Password) : null;
            Totp = deserializedData.Totp != null ? new CipherString(deserializedData.Totp) : null;
            Uris = deserializedData.Uris?.Select(u => new LoginUri(u));
        }

        public IEnumerable<LoginUri> Uris { get; set; }
        public CipherString Username { get; set; }
        public CipherString Password { get; set; }
        public CipherString Totp { get; set; }
    }
}
