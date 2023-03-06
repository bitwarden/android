using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IOrganizationService
    {
        Task<Organization> GetAsync(string id);
        Task<Organization> GetByIdentifierAsync(string identifier);
        Task<List<Organization>> GetAllAsync(string userId = null);
        Task ReplaceAsync(Dictionary<string, OrganizationData> organizations);
        Task ClearAllAsync(string userId);
        Task<OrganizationDomainSsoDetailsResponse> GetClaimedOrganizationDomainAsync(string userEmail);
    }
}
