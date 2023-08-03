using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IKeyConnectorService
    {
        Task SetUsesKeyConnector(bool usesKeyConnector);
        Task<bool> GetUsesKeyConnector();
        Task<bool> UserNeedsMigration();
        Task MigrateUser();
        Task GetAndSetKey(string url);
        Task<Organization> GetManagingOrganization();
        Task ConvertNewUserToKeyConnectorAsync(string orgId, IdentityTokenResponse tokenResponse);
    }
}
