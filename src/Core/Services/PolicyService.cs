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
        private const string Keys_PoliciesPrefix = "policies_{0}";
        
        private readonly IStorageService _storageService;
        private readonly IUserService _userService;

        private IEnumerable<Policy> _policyCache;

        public PolicyService(
            IStorageService storageService,
            IUserService userService)
        {
            _storageService = storageService;
            _userService = userService;
        }
        
        public void ClearCache()
        {
            _policyCache = null;
        }

        public async Task<IEnumerable<Policy>> GetAll(PolicyType? type)
        {
            if (_policyCache == null)
            {
                var userId = await _userService.GetUserIdAsync();
                var policies = await _storageService.GetAsync<Dictionary<string, PolicyData>>(
                    string.Format(Keys_PoliciesPrefix, userId));
                if (policies == null)
                {
                    return null;
                }
                _policyCache = policies.Select(p => new Policy(policies[p.Key]));
            }

            if (type != null)
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
            await _storageService.SaveAsync(string.Format(Keys_PoliciesPrefix, userId), policies);
            _policyCache = null;
        }

        public async Task Clear(string userId)
        {
            await _storageService.RemoveAsync(string.Format(Keys_PoliciesPrefix, userId));
            _policyCache = null;
        }
    }
}
