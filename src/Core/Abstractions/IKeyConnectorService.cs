using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IKeyConnectorService
    {
        Task SetUsesKeyConnector(bool usesKeyConnector);
        Task<bool> GetUsesKeyConnectorAsync();
        Task<bool> UserNeedsMigration();
        Task MigrateUser();
        Task GetAndSetKeyAsync(string url);
        Task<Organization> GetManagingOrganization();
        Task ConvertNewUserToKeyConnectorAsync(string orgId, IdentityTokenResponse tokenResponse);
    }
}
