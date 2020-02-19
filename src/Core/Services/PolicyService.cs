using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;

namespace Bit.Core.Services
{
    public class PolicyService : IPolicyService
    {
        private readonly IStorageService _storageService;
        private readonly IUserService _userService;

        private List<Policy> _policyCache;

        public PolicyService(
            IStorageService storageService,
            IUserService userService)
        {
            _storageService = storageService;
            _userService = userService;
        }

        private class Keys
        {
            public static string PoliciesPrefix = "policies_";
        }

        public void ClearCache()
        {
            _policyCache = null;
        }

        public async Task<List<Policy>> GetAll(PolicyType? type)
        {
            if(_policyCache == null)
            {
                var userId = await _userService.GetUserIdAsync();
                var policies =
                    await _storageService.GetAsync<Dictionary<string, PolicyData>>(Keys.PoliciesPrefix + userId);
                var response = new List<Policy>();
                foreach(var id in policies)
                {
                    if(policies.GetType().GetProperty(id.Key) != null)
                    {
                        response.Add(new Policy(policies[id.Key]));
                    }
                }

                _policyCache = response;
            }

            if(type != null)
            {
                return _policyCache.Where(p => p.Type == type).ToList();
            }
            else
            {
                return _policyCache;
            }
        }

        public async Task Replace(Dictionary<string, PolicyData> policies)
        {
            var userId = await _userService.GetUserIdAsync();
            await _storageService.SaveAsync(Keys.PoliciesPrefix + userId, policies);
            _policyCache = null;
        }

        public async Task Clear(string userId)
        {
            await _storageService.RemoveAsync(Keys.PoliciesPrefix + userId);
            _policyCache = null;
        }
    }
}
