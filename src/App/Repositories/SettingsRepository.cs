using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class SettingsRepository : Repository<SettingsData, string>, ISettingsRepository
    {
        public SettingsRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<IEnumerable<string>>> GetEquivablentDomains(string userId)
        {
            var equivalentDomainsJson = Connection.Table<SettingsData>().Where(f => f.Id == userId)
                .Select(f => f.EquivalentDomains).FirstOrDefault();

            if(string.IsNullOrWhiteSpace(equivalentDomainsJson))
            {
                return Task.FromResult<IEnumerable<IEnumerable<string>>>(null);
            }

            var equivalentDomains = JsonConvert.DeserializeObject<IEnumerable<IEnumerable<string>>>(equivalentDomainsJson);
            return Task.FromResult(equivalentDomains);
        }
    }
}
