using Bit.App.Models.Api;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models.Data
{
    public abstract class CipherDataModel
    {
        public CipherDataModel() { }

        public CipherDataModel(CipherResponse cipher)
        {
            Name = cipher.Name;
            Notes = cipher.Notes;
            Fields = cipher.Fields?.Select(f => new FieldDataModel(f));
            PasswordHistory = cipher.PasswordHistory?.Select(h => new PasswordHistoryDataModel(h));
        }

        public string Name { get; set; }
        public string Notes { get; set; }
        public IEnumerable<FieldDataModel> Fields { get; set; }
        public IEnumerable<PasswordHistoryDataModel> PasswordHistory { get; set; }
    }
}
