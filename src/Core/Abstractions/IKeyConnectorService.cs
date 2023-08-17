using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IKeyConnectorService
    {
        Task SetUsesKeyConnectorAsync(bool usesKeyConnector);
        Task<bool> GetUsesKeyConnectorAsync();
        Task<bool> UserNeedsMigrationAsync();
        Task MigrateUserAsync();
        Task SetMasterKeyFromUrlAsync(string url);
        Task<Organization> GetManagingOrganizationAsync();
        Task ConvertNewUserToKeyConnectorAsync(string orgId, IdentityTokenResponse tokenResponse);
    }
}
