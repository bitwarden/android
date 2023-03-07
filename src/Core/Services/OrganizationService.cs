using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class OrganizationService : IOrganizationService
    {
        private readonly IStateService _stateService;
        private readonly IApiService _apiService;

        public OrganizationService(IStateService stateService, IApiService apiService)
        {
            _stateService = stateService;
            _apiService = apiService;
        }

        public async Task<Organization> GetAsync(string id)
        {
            var organizations = await _stateService.GetOrganizationsAsync();
            if (organizations == null || !organizations.ContainsKey(id))
            {
                return null;
            }
            return new Organization(organizations[id]);
        }

        public async Task<Organization> GetByIdentifierAsync(string identifier)
        {
            var organizations = await GetAllAsync();
            if (organizations == null || organizations.Count == 0)
            {
                return null;
            }
            return organizations.FirstOrDefault(o => o.Identifier == identifier);
        }

        public async Task<List<Organization>> GetAllAsync(string userId = null)
        {
            var organizations = await _stateService.GetOrganizationsAsync(userId);
            return organizations?.Select(o => new Organization(o.Value)).ToList() ?? new List<Organization>();
        }

        public async Task ReplaceAsync(Dictionary<string, OrganizationData> organizations)
        {
            await _stateService.SetOrganizationsAsync(organizations);
        }

        public async Task ClearAllAsync(string userId)
        {
            await _stateService.SetOrganizationsAsync(null, userId);
        }

        public async Task<OrganizationDomainSsoDetailsResponse> GetClaimedOrganizationDomainAsync(string userEmail)
        {
            try
            {
                if (string.IsNullOrEmpty(userEmail))
                {
                    return null;
                }

                return await _apiService.GetOrgDomainSsoDetailsAsync(userEmail);
            }
            catch (ApiException ex) when (ex.Error?.StatusCode == HttpStatusCode.NotFound)
            {
                // this is a valid case so there is no need to show an error
                return null;
            }
        }
    }
}
