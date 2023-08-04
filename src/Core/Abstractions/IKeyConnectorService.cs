using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IKeyConnectorService
    {
        Task SetUsesKeyConnectorAsync(bool usesKeyConnector);
        Task<bool> GetUsesKeyConnectorAsync();
        Task<bool> UserNeedsMigrationAsync();
        Task MigrateUserAsync();
        Task GetAndSetMasterKeyAsync(string url);
        Task<Organization> GetManagingOrganizationAsync();
    }
}
