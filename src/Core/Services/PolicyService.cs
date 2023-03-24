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

        private IEnumerable<Policy> _policyCache;

        public PolicyService(
            IStateService stateService,
            IOrganizationService organizationService)
        {
            _stateService = stateService;
            _organizationService = organizationService;
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

            return _policyCache;
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

        public async Task<PasswordGeneratorPolicyOptions> GetPasswordGeneratorPolicyOptionsAsync()
        {
            var policies = await GetAll(PolicyType.PasswordGenerator);
            if (policies == null)
            {
                return null;
            }

            var actualPolicies = policies.Where(p => p.Enabled && p.Data != null);
            if (!actualPolicies.Any())
            {
                return null;
            }

            var enforcedOptions = new PasswordGeneratorPolicyOptions();

            foreach (var currentPolicy in actualPolicies)
            {
                var defaultType = GetPolicyString(currentPolicy, "defaultType");
                if (defaultType != null && enforcedOptions.DefaultType != "password")
                {
                    enforcedOptions.DefaultType = defaultType;
                }

                var minLength = GetPolicyInt(currentPolicy, "minLength");
                if (minLength != null && (int)(long)minLength > enforcedOptions.MinLength)
                {
                    enforcedOptions.MinLength = (int)(long)minLength;
                }

                var useUpper = GetPolicyBool(currentPolicy, "useUpper");
                if (useUpper != null && (bool)useUpper)
                {
                    enforcedOptions.UseUppercase = true;
                }

                var useLower = GetPolicyBool(currentPolicy, "useLower");
                if (useLower != null && (bool)useLower)
                {
                    enforcedOptions.UseLowercase = true;
                }

                var useNumbers = GetPolicyBool(currentPolicy, "useNumbers");
                if (useNumbers != null && (bool)useNumbers)
                {
                    enforcedOptions.UseNumbers = true;
                }

                var minNumbers = GetPolicyInt(currentPolicy, "minNumbers");
                if (minNumbers != null && (int)(long)minNumbers > enforcedOptions.NumberCount)
                {
                    enforcedOptions.NumberCount = (int)(long)minNumbers;
                }

                var useSpecial = GetPolicyBool(currentPolicy, "useSpecial");
                if (useSpecial != null && (bool)useSpecial)
                {
                    enforcedOptions.UseSpecial = true;
                }

                var minSpecial = GetPolicyInt(currentPolicy, "minSpecial");
                if (minSpecial != null && (int)(long)minSpecial > enforcedOptions.SpecialCount)
                {
                    enforcedOptions.SpecialCount = (int)(long)minSpecial;
                }

                var minNumberWords = GetPolicyInt(currentPolicy, "minNumberWords");
                if (minNumberWords != null && (int)(long)minNumberWords > enforcedOptions.MinNumberOfWords)
                {
                    enforcedOptions.MinNumberOfWords = (int)(long)minNumberWords;
                }

                var capitalize = GetPolicyBool(currentPolicy, "capitalize");
                if (capitalize != null && (bool)capitalize)
                {
                    enforcedOptions.Capitalize = true;
                }

                var includeNumber = GetPolicyBool(currentPolicy, "includeNumber");
                if (includeNumber != null && (bool)includeNumber)
                {
                    enforcedOptions.IncludeNumber = true;
                }
            }

            return enforcedOptions;
        }
    }
}
