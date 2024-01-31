using System.Collections.Generic;
using System.Linq;
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
            nameof(Uri),
            nameof(UriChecksum)
        };

        public LoginUri() { }

        public LoginUri(LoginUriData obj, bool alreadyEncrypted = false)
        {
            Match = obj.Match;
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
        }

        public EncString Uri { get; set; }
        public UriMatchType? Match { get; set; }
        public EncString UriChecksum { get; set; }

        public Task<LoginUriView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            return DecryptObjAsync(new LoginUriView(this), this, _map.Where(m => m != nameof(UriChecksum)).ToHashSet<string>(), orgId, key);
        }

        public LoginUriData ToLoginUriData()
        {
            var u = new LoginUriData();
            BuildDataModel(this, u, _map, new HashSet<string> { "Match" });
            return u;
        }
    }
}
