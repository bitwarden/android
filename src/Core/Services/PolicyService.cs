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

        public async Task<Policy> FirstOrDefault(PolicyType? type, string userId = null)
        {
            return (await GetAll(type, userId)).FirstOrDefault();
        }

        public async Task Replace(Dictionary<string, PolicyData> policies, string userId = null)
        {
            await _stateService.SetEncryptedPoliciesAsync(policies, userId);
            _policyCache = null;

            var vaultTimeoutPolicy = policies.FirstOrDefault(p => p.Value.Type == PolicyType.MaximumVaultTimeout);
            if (vaultTimeoutPolicy.Value != null)
            {
                await UpdateVaultTimeoutFromPolicyAsync(new Policy(vaultTimeoutPolicy.Value));
            }
        }

        public async Task ClearAsync(string userId)
        {
            await _stateService.SetEncryptedPoliciesAsync(null, userId);
            _policyCache = null;
        }

        public async Task UpdateVaultTimeoutFromPolicyAsync(Policy policy, string userId = null)
        {
            var policyTimeout = policy.GetInt(Policy.MINUTES_KEY);
            if (policyTimeout != null)
            {
                var vaultTimeout = await _stateService.GetVaultTimeoutAsync(userId);
                var timeout = vaultTimeout.HasValue ? Math.Min(vaultTimeout.Value, policyTimeout.Value) : policyTimeout.Value;
                if (timeout < 0)
                {
                    timeout = policyTimeout.Value;
                }
                if (vaultTimeout != timeout)
                {
                    await _stateService.SetVaultTimeoutAsync(timeout, userId);
                }
            }

            var policyAction = policy.GetString(Policy.ACTION_KEY);
            if (!string.IsNullOrEmpty(policyAction))
            {
                var vaultTimeoutAction = await _stateService.GetVaultTimeoutActionAsync(userId);
                var action = policyAction == Policy.ACTION_LOCK ? VaultTimeoutAction.Lock : VaultTimeoutAction.Logout;
                if (vaultTimeoutAction != action)
                {
                    await _stateService.SetVaultTimeoutActionAsync(action, userId);
                }
            }
        }

        public async Task<MasterPasswordPolicyOptions> GetMasterPasswordPolicyOptions(
            IEnumerable<Policy> policies = null, string userId = null)
        {
            if (policies == null)
            {
                policies = await GetAll(PolicyType.MasterPassword, userId);
                if (policies == null)
                {
                    return null;
                }
            }
            else
            {
                policies = policies.Where(p => p.Type == PolicyType.MasterPassword);
            }

            policies = policies.Where(p => p.Enabled && p.Data != null);

            if (!policies.Any())
            {
                return null;
            }

            var enforcedOptions = new MasterPasswordPolicyOptions();

            foreach (var currentPolicy in policies)
            {
                var minComplexity = currentPolicy.GetInt("minComplexity");
                if (minComplexity > enforcedOptions.MinComplexity)
                {
                    enforcedOptions.MinComplexity = minComplexity.Value;
                }

                var minLength = currentPolicy.GetInt("minLength");
                if (minLength > enforcedOptions.MinLength)
                {
                    enforcedOptions.MinLength = minLength.Value;
                }

                if (currentPolicy.GetBool("requireUpper") == true)
                {
                    enforcedOptions.RequireUpper = true;
                }

                if (currentPolicy.GetBool("requireLower") == true)
                {
                    enforcedOptions.RequireLower = true;
                }

                if (currentPolicy.GetBool("requireNumbers") == true)
                {
                    enforcedOptions.RequireNumbers = true;
                }

                if (currentPolicy.GetBool("requireSpecial") == true)
                {
                    enforcedOptions.RequireSpecial = true;
                }

                var enforceOnLogin = currentPolicy.GetBool("enforceOnLogin");
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
            resetPasswordPolicyOptions.AutoEnrollEnabled = policy.GetBool("autoEnrollEnabled") ?? false;

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
                var defaultType = currentPolicy.GetString("defaultType");
                if (defaultType != null && enforcedOptions.DefaultType != PasswordGenerationOptions.TYPE_PASSWORD)
                {
                    enforcedOptions.DefaultType = defaultType;
                }

                var minLength = currentPolicy.GetInt("minLength");
                if (minLength > enforcedOptions.MinLength)
                {
                    enforcedOptions.MinLength = minLength.Value;
                }

                if (currentPolicy.GetBool("useUpper") == true)
                {
                    enforcedOptions.UseUppercase = true;
                }

                if (currentPolicy.GetBool("useLower") == true)
                {
                    enforcedOptions.UseLowercase = true;
                }

                if (currentPolicy.GetBool("useNumbers") == true)
                {
                    enforcedOptions.UseNumbers = true;
                }

                var minNumbers = currentPolicy.GetInt("minNumbers");
                if (minNumbers > enforcedOptions.NumberCount)
                {
                    enforcedOptions.NumberCount = minNumbers.Value;
                }

                if (currentPolicy.GetBool("useSpecial") == true)
                {
                    enforcedOptions.UseSpecial = true;
                }

                var minSpecial = currentPolicy.GetInt("minSpecial");
                if (minSpecial > enforcedOptions.SpecialCount)
                {
                    enforcedOptions.SpecialCount = minSpecial.Value;
                }

                var minNumberWords = currentPolicy.GetInt("minNumberWords");
                if (minNumberWords > enforcedOptions.MinNumberOfWords)
                {
                    enforcedOptions.MinNumberOfWords = minNumberWords.Value;
                }

                if (currentPolicy.GetBool("capitalize") == true)
                {
                    enforcedOptions.Capitalize = true;
                }

                if (currentPolicy.GetBool("includeNumber") == true)
                {
                    enforcedOptions.IncludeNumber = true;
                }
            }

            return enforcedOptions;
        }
    }
}
