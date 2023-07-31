using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IKeyConnectorService
    {
        Task SetUsesKeyConnector(bool usesKeyConnector);
        Task<bool> GetUsesKeyConnectorAsync();
        Task<bool> UserNeedsMigration();
        Task MigrateUser();
        Task GetAndSetKey(string url);
        Task<Organization> GetManagingOrganization();
    }
}
