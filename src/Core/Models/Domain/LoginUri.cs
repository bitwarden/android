using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class LoginUri : Domain
    {
        public LoginUri() { }

        public LoginUri(LoginUriData obj, bool alreadyEncrypted = false)
        {
            Match = obj.Match;
            BuildDomainModel(this, obj, new HashSet<string>
            {
                "Uri"
            }, alreadyEncrypted);
        }

        public CipherString Uri { get; set; }
        public UriMatchType? Match { get; set; }

        public Task<LoginUriView> DecryptAsync(string orgId)
        {
            return DecryptObjAsync(new LoginUriView(this), this, new HashSet<string>
            {
                "Uri"
            }, orgId);
        }

        public LoginUriData ToLoginUriData()
        {
            var u = new LoginUriData();
            BuildDataModel(this, u, new HashSet<string>
            {
                "Uri"
            }, new HashSet<string> { "Match" });
            return u;
        }
    }
}
