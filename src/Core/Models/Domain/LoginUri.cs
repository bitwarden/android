using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class LoginUri : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "Uri"
        };

        public LoginUri() { }

        public LoginUri(LoginUriData obj, bool alreadyEncrypted = false)
        {
            Match = obj.Match;
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
        }

        public EncString Uri { get; set; }
        public UriMatchType? Match { get; set; }

        public Task<LoginUriView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            return DecryptObjAsync(new LoginUriView(this), this, _map, orgId, key);
        }

        public LoginUriData ToLoginUriData()
        {
            var u = new LoginUriData();
            BuildDataModel(this, u, _map, new HashSet<string> { "Match" });
            return u;
        }
    }
}
