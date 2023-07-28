using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public abstract class VaultFilterViewModel : BaseViewModel
    {
        protected abstract ICipherService cipherService { get; }
        protected abstract IPolicyService policyService { get; }
        protected abstract IOrganizationService organizationService { get; }
        protected abstract ILogger logger { get; }

        protected bool _showVaultFilter;
        protected bool _personalOwnershipPolicyApplies;
        protected string _vaultFilterSelection;
        protected List<Organization> _organizations;

        public VaultFilterViewModel()
        {
            VaultFilterCommand = new AsyncCommand(VaultFilterOptionsAsync,
                onException: ex => logger.Exception(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand VaultFilterCommand { get; set; }

        public bool ShowVaultFilter
        {
            get => _showVaultFilter;
            set => SetProperty(ref _showVaultFilter, value);
        }

        public string VaultFilterDescription
        {
            get
            {
                if (_vaultFilterSelection == null || _vaultFilterSelection == AppResources.AllVaults)
                {
                    return string.Format(AppResources.VaultFilterDescription, AppResources.All);
                }
                return string.Format(AppResources.VaultFilterDescription, _vaultFilterSelection);
            }
            set => SetProperty(ref _vaultFilterSelection, value);
        }

        public string GetVaultFilterOrgId()
        {
            return _organizations?.FirstOrDefault(o => o.Name == _vaultFilterSelection)?.Id;
        }

        protected bool IsVaultFilterMyVault => _vaultFilterSelection == AppResources.MyVault;

        protected bool IsVaultFilterOrgVault => _vaultFilterSelection != AppResources.AllVaults &&
                                                _vaultFilterSelection != AppResources.MyVault;

        protected async Task InitVaultFilterAsync(bool shouldUpdateShowVaultFilter)
        {
            _organizations = await organizationService.GetAllAsync();
            if (_organizations?.Any() ?? false)
            {
                _personalOwnershipPolicyApplies = await policyService.PolicyAppliesToUser(PolicyType.PersonalOwnership);
                var singleOrgPolicyApplies = await policyService.PolicyAppliesToUser(PolicyType.OnlyOrg);
                if (_vaultFilterSelection == null || (_personalOwnershipPolicyApplies && singleOrgPolicyApplies))
                {
                    VaultFilterDescription = AppResources.AllVaults;
                }
            }
            if (shouldUpdateShowVaultFilter)
            {
                await Task.Delay(100);
                ShowVaultFilter = await policyService.ShouldShowVaultFilterAsync();
            }
        }

        protected async Task<List<CipherView>> GetAllCiphersAsync()
        {
            var decCiphers = await cipherService.GetAllDecryptedAsync();
            if (IsVaultFilterMyVault)
            {
                return decCiphers.Where(c => c.OrganizationId == null).ToList();
            }
            if (IsVaultFilterOrgVault)
            {
                var orgId = GetVaultFilterOrgId();
                return decCiphers.Where(c => c.OrganizationId == orgId).ToList();
            }
            return decCiphers;
        }

        protected async Task VaultFilterOptionsAsync()
        {
            var options = new List<string> { AppResources.AllVaults };
            if (!_personalOwnershipPolicyApplies)
            {
                options.Add(AppResources.MyVault);
            }
            if (_organizations.Any())
            {
                options.AddRange(_organizations.OrderBy(o => o.Name).Select(o => o.Name));
            }
            var selection = await Page.DisplayActionSheet(AppResources.FilterByVault, AppResources.Cancel, null,
                options.ToArray());
            if (selection == null || selection == AppResources.Cancel ||
                (_vaultFilterSelection == null && selection == AppResources.AllVaults) ||
                (_vaultFilterSelection != null && _vaultFilterSelection == selection))
            {
                return;
            }
            VaultFilterDescription = selection;
            await OnVaultFilterSelectedAsync();
        }

        protected abstract Task OnVaultFilterSelectedAsync();
    }
}
