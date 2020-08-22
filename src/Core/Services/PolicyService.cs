using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
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

        public async Task<MasterPasswordPolicyOptions> GetMasterPasswordPolicyOptions(
            IEnumerable<Policy> policies = null)
        {
            MasterPasswordPolicyOptions enforcedOptions = null;

            if (policies == null)
            {
                policies = await GetAll(PolicyType.MasterPassword);
            }
            else
            {
                policies = policies.Where(p => p.Type == PolicyType.MasterPassword);
            }

            if (policies == null || !policies.Any())
            {
                return enforcedOptions;
            }

            foreach (var currentPolicy in policies)
            {
                if (!currentPolicy.Enabled || currentPolicy.Data == null)
                {
                    continue;
                }

                if (enforcedOptions == null)
                {
                    enforcedOptions = new MasterPasswordPolicyOptions();
                }

                var minComplexity = GetPolicyInt(currentPolicy, "minComplexity");
                if (minComplexity != null && (int)(long)minComplexity > enforcedOptions.MinComplexity)
                {
                    enforcedOptions.MinComplexity = (int)(long)minComplexity;
                }

                var minLength = GetPolicyInt(currentPolicy, "minLength");
                if (minLength != null && (int)(long)minLength > enforcedOptions.MinLength)
                {
                    enforcedOptions.MinLength = (int)(long)minLength;
                }

                var requireUpper = GetPolicyBool(currentPolicy, "requireUpper");
                if (requireUpper != null && (bool)requireUpper)
                {
                    enforcedOptions.RequireUpper = true;
                }

                var requireLower = GetPolicyBool(currentPolicy, "requireLower");
                if (requireLower != null && (bool)requireLower)
                {
                    enforcedOptions.RequireLower = true;
                }

                var requireNumbers = GetPolicyBool(currentPolicy, "requireNumbers");
                if (requireNumbers != null && (bool)requireNumbers)
                {
                    enforcedOptions.RequireNumbers = true;
                }

                var requireSpecial = GetPolicyBool(currentPolicy, "requireSpecial");
                if (requireSpecial != null && (bool)requireSpecial)
                {
                    enforcedOptions.RequireSpecial = true;
                }
            }

            return enforcedOptions;
        }

        public async Task<bool> EvaluateMasterPassword(int passwordStrength, string newPassword,
            MasterPasswordPolicyOptions enforcedPolicyOptions)
        {
            if (enforcedPolicyOptions == null)
            {
                return true;
            }

            if (enforcedPolicyOptions.MinComplexity > 0 && enforcedPolicyOptions.MinComplexity > passwordStrength)
            {
                return false;
            }

            if (enforcedPolicyOptions.MinLength > 0 && enforcedPolicyOptions.MinLength > newPassword.Length)
            {
                return false;
            }

            if (enforcedPolicyOptions.RequireUpper && newPassword.ToLower() == newPassword)
            {
                return false;
            }

            if (enforcedPolicyOptions.RequireLower && newPassword.ToUpper() == newPassword)
            {
                return false;
            }

            if (enforcedPolicyOptions.RequireNumbers && !newPassword.Any(char.IsDigit))
            {
                return false;
            }

            if (enforcedPolicyOptions.RequireSpecial && !Regex.IsMatch(newPassword, "^.*[!@#$%\\^&*].*$"))
            {
                return false;
            }

            return true;
        }

        private int? GetPolicyInt(Policy policy, string key)
        {
            if (policy.Data.ContainsKey(key))
            {
                var value = policy.Data[key];
                if (value != null)
                {
                    return (int)(long)value;
                }
            }
            return null;
        }

        private bool? GetPolicyBool(Policy policy, string key)
        {
            if (policy.Data.ContainsKey(key))
            {
                var value = policy.Data[key];
                if (value != null)
                {
                    return (bool)value;
                }
            }
            return null;
        }

        private string GetPolicyString(Policy policy, string key)
        {
            if (policy.Data.ContainsKey(key))
            {
                var value = policy.Data[key];
                if (value != null)
                {
                    return (string)value;
                }
            }
            return null;
        }
    }
}
