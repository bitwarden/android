using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class SettingsRepository : Repository<SettingsData, string>, ISettingsRepository
    {
        public SettingsRepository(ISqlService sqlService)
            : base(sqlService)
        { }
    }
}
