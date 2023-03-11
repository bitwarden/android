using System;
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
        private readonly IStateService _stateService;
        private readonly IOrganizationService _organizationService;
        private readonly IPasswordGenerationService _passwordGenerationService;

        private IEnumerable<Policy> _policyCache;

        public PolicyService(
            IStateService stateService,
            IOrganizationService organizationService,
            IPasswordGenerationService passwordGenerationService)
        {
            _stateService = stateService;
            _organizationService = organizationService;
            _passwordGenerationService = passwordGenerationService;
        }

        public void ClearCache()
        {
            _policyCache = null;
        }

        public async Task<IEnumerable<Policy>> GetAll(PolicyType? type, string userId = null)
        {
            if (_policyCache == null)
            {
                var policies = await _stateService.GetEncryptedPoliciesAsync(userId);
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

        public async Task Replace(Dictionary<string, PolicyData> policies, string userId = null)
        {
            await _stateService.SetEncryptedPoliciesAsync(policies, userId);
            _policyCache = null;
        }

        public async Task ClearAsync(string userId)
        {
            await _stateService.SetEncryptedPoliciesAsync(null, userId);
            _policyCache = null;
        }

        public async Task<MasterPasswordPolicyOptions> GetMasterPasswordPolicyOptions(
            IEnumerable<Policy> policies = null, string userId = null)
        {
            MasterPasswordPolicyOptions enforcedOptions = null;

            if (policies == null)
            {
                policies = await GetAll(PolicyType.MasterPassword, userId);
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
                if (requireUpper == true)
                {
                    enforcedOptions.RequireUpper = true;
                }

                var requireLower = GetPolicyBool(currentPolicy, "requireLower");
                if (requireLower == true)
                {
                    enforcedOptions.RequireLower = true;
                }

                var requireNumbers = GetPolicyBool(currentPolicy, "requireNumbers");
                if (requireNumbers == true)
                {
                    enforcedOptions.RequireNumbers = true;
                }

                var requireSpecial = GetPolicyBool(currentPolicy, "requireSpecial");
                if (requireSpecial == true)
                {
                    enforcedOptions.RequireSpecial = true;
                }

                var enforceOnLogin = GetPolicyBool(currentPolicy, "enforceOnLogin");
                if (enforceOnLogin == true)
                {
                    enforcedOptions.EnforceOnLogin = true;
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
        
        public async Task<bool> RequirePasswordChangeOnLoginAsync(string masterPassword, string email,
            MasterPasswordPolicyOptions enforcedOptions)
        {
            // No policy to enforce on login/unlock
            if (!(enforcedOptions is { EnforceOnLogin: true }))
            {
                return false;
            }
            
            var strength = _passwordGenerationService.PasswordStrength(
                masterPassword, _passwordGenerationService.GetPasswordStrengthUserInput(email))?.Score;

            if (!strength.HasValue)
            {
                return false;
            }
            
            return !await EvaluateMasterPassword(
                strength.Value,
                masterPassword,
                enforcedOptions
            );
        }

        public Tuple<ResetPasswordPolicyOptions, bool> GetResetPasswordPolicyOptions(IEnumerable<Policy> policies,
            string orgId)
        {
            var resetPasswordPolicyOptions = new ResetPasswordPolicyOptions();

            if (policies == null || orgId == null)
            {
                return new Tuple<ResetPasswordPolicyOptions, bool>(resetPasswordPolicyOptions, false);
            }

            var policy = policies.FirstOrDefault(p =>
                p.OrganizationId == orgId && p.Type == PolicyType.ResetPassword && p.Enabled);
            resetPasswordPolicyOptions.AutoEnrollEnabled = GetPolicyBool(policy, "autoEnrollEnabled") ?? false;

            return new Tuple<ResetPasswordPolicyOptions, bool>(resetPasswordPolicyOptions, policy != null);
        }

        public async Task<bool> PolicyAppliesToUser(PolicyType policyType, Func<Policy, bool> policyFilter = null,
            string userId = null)
        {
            var policies = await GetAll(policyType, userId);
            if (policies == null)
            {
                return false;
            }
            var organizations = await _organizationService.GetAllAsync(userId);

            IEnumerable<Policy> filteredPolicies;

            if (policyFilter != null)
            {
                filteredPolicies = policies.Where(p => p.Enabled && policyFilter(p));
            }
            else
            {
                filteredPolicies = policies.Where(p => p.Enabled);
            }

            var policySet = new HashSet<string>(filteredPolicies.Select(p => p.OrganizationId));

            return organizations.Any(o =>
                o.Enabled &&
                o.Status >= OrganizationUserStatusType.Accepted &&
                o.UsePolicies &&
                !isExcemptFromPolicies(o, policyType) &&
                policySet.Contains(o.Id));
        }

        private bool isExcemptFromPolicies(Organization organization, PolicyType policyType)
        {
            if (policyType == PolicyType.MaximumVaultTimeout)
            {
                return organization.Type == OrganizationUserType.Owner;
            }

            return organization.isExemptFromPolicies;
        }

        public int? GetPolicyInt(Policy policy, string key)
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

        public async Task<bool> ShouldShowVaultFilterAsync()
        {
            var personalOwnershipPolicyApplies = await PolicyAppliesToUser(PolicyType.PersonalOwnership);
            var singleOrgPolicyApplies = await PolicyAppliesToUser(PolicyType.OnlyOrg);
            if (personalOwnershipPolicyApplies && singleOrgPolicyApplies)
            {
                return false;
            }
            var organizations = await _organizationService.GetAllAsync();
            return organizations?.Any() ?? false;
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
