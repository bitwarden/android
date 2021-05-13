using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class PasswordHistory : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "Password"
        };

        public PasswordHistory() { }

        public PasswordHistory(PasswordHistoryData obj, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
            LastUsedDate = obj.LastUsedDate.GetValueOrDefault();
        }

        public EncString Password { get; set; }
        public DateTime LastUsedDate { get; set; }

        public Task<PasswordHistoryView> DecryptAsync(string orgId)
        {
            return DecryptObjAsync(new PasswordHistoryView(this), this, _map, orgId);
        }

        public PasswordHistoryData ToPasswordHistoryData()
        {
            var ph = new PasswordHistoryData();
            ph.LastUsedDate = LastUsedDate;
            BuildDataModel(this, ph, _map);
            return ph;
        }
    }
}
