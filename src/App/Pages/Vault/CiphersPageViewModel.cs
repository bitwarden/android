﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class CiphersPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICipherService _cipherService;
        private readonly ISearchService _searchService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IStateService _stateService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IOrganizationService _organizationService;
        private readonly IPolicyService _policyService;
        private CancellationTokenSource _searchCancellationTokenSource;
        private readonly ILogger _logger;

        private bool _showVaultFilter;
        private string _vaultFilterSelection;
        private bool _showNoData;
        private bool _showList;
        private bool _websiteIconsEnabled;
        private List<Organization> _organizations;

        public CiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            Ciphers = new ExtendedObservableCollection<CipherView>();
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
            VaultFilterCommand = new AsyncCommand(VaultFilterOptionsAsync,
                onException: ex => _logger.Exception(ex),
                allowsMultipleExecutions: false);
        }

        public Command CipherOptionsCommand { get; set; }
        public ICommand VaultFilterCommand { get; }
        public ExtendedObservableCollection<CipherView> Ciphers { get; set; }
        public Func<CipherView, bool> Filter { get; set; }
        public string AutofillUrl { get; set; }
        public bool Deleted { get; set; }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value, additionalPropertyNames: new string[]
            {
                nameof(ShowSearchDirection)
            });
        }

        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value, additionalPropertyNames: new string[]
            {
                nameof(ShowSearchDirection)
            });
        }
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

        public bool ShowSearchDirection => !ShowList && !ShowNoData;

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public async Task InitAsync()
        {
            _organizations = await _organizationService.GetAllAsync();
            ShowVaultFilter = await _policyService.ShouldShowVaultFilterAsync();
            if (ShowVaultFilter && _vaultFilterSelection == null)
            {
                _vaultFilterSelection = AppResources.AllVaults;
            }
            WebsiteIconsEnabled = !(await _stateService.GetDisableFaviconAsync()).GetValueOrDefault();
            PerformSearchIfPopulated();
        }

        public void Search(string searchText, int? timeout = null)
        {
            var previousCts = _searchCancellationTokenSource;
            var cts = new CancellationTokenSource();
            Task.Run(async () =>
            {
                List<CipherView> ciphers = null;
                var searchable = !string.IsNullOrWhiteSpace(searchText) && searchText.Length > 1;
                if (searchable)
                {
                    if (timeout != null)
                    {
                        await Task.Delay(timeout.Value);
                    }
                    if (searchText != (Page as CiphersPage).SearchBar.Text)
                    {
                        return;
                    }
                    else
                    {
                        previousCts?.Cancel();
                    }
                    try
                    {
                        var vaultFilteredCiphers = await GetAllCiphersAsync();
                        ciphers = await _searchService.SearchCiphersAsync(searchText,
                            Filter ?? (c => c.IsDeleted == Deleted), vaultFilteredCiphers, cts.Token);
                        cts.Token.ThrowIfCancellationRequested();
                    }
                    catch (OperationCanceledException)
                    {
                        return;
                    }
                }
                if (ciphers == null)
                {
                    ciphers = new List<CipherView>();
                }
                Device.BeginInvokeOnMainThread(() =>
                {
                    Ciphers.ResetWithRange(ciphers);
                    ShowNoData = searchable && Ciphers.Count == 0;
                    ShowList = searchable && !ShowNoData;
                });
            }, cts.Token);
            _searchCancellationTokenSource = cts;
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
            string selection = null;
            if (!string.IsNullOrWhiteSpace(AutofillUrl))
            {
                var options = new List<string> { AppResources.Autofill };
                if (cipher.Type == CipherType.Login &&
                    Xamarin.Essentials.Connectivity.NetworkAccess != Xamarin.Essentials.NetworkAccess.None)
                {
                    options.Add(AppResources.AutofillAndSave);
                }
                options.Add(AppResources.View);
                selection = await Page.DisplayActionSheet(AppResources.AutofillOrView, AppResources.Cancel, null,
                    options.ToArray());
            }
            if (selection == AppResources.View || string.IsNullOrWhiteSpace(AutofillUrl))
            {
                var page = new ViewPage(cipher.Id);
                await Page.Navigation.PushModalAsync(new NavigationPage(page));
            }
            else if (selection == AppResources.Autofill || selection == AppResources.AutofillAndSave)
            {
                if (selection == AppResources.AutofillAndSave)
                {
                    var uris = cipher.Login?.Uris?.ToList();
                    if (uris == null)
                    {
                        uris = new List<LoginUriView>();
                    }
                    uris.Add(new LoginUriView
                    {
                        Uri = AutofillUrl,
                        Match = null
                    });
                    cipher.Login.Uris = uris;
                    try
                    {
                        await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                        await _cipherService.SaveWithServerAsync(await _cipherService.EncryptAsync(cipher));
                        await _deviceActionService.HideLoadingAsync();
                    }
                    catch (ApiException e)
                    {
                        await _deviceActionService.HideLoadingAsync();
                        if (e?.Error != null)
                        {
                            await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                                AppResources.AnErrorHasOccurred);
                        }
                    }
                }
                if (_deviceActionService.SystemMajorVersion() < 21)
                {
                    await Utilities.AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
                }
                else
                {
                    _deviceActionService.Autofill(cipher);
                }
            }
        }

        private void PerformSearchIfPopulated()
        {
            if (!string.IsNullOrWhiteSpace((Page as CiphersPage).SearchBar.Text))
            {
                Search((Page as CiphersPage).SearchBar.Text, 200);
            }
        }

        private async Task VaultFilterOptionsAsync()
        {
            var options = new List<string> { AppResources.AllVaults, AppResources.MyVault };
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
            PerformSearchIfPopulated();
        }

        private async Task<List<CipherView>> GetAllCiphersAsync()
        {
            var decCiphers = await _cipherService.GetAllDecryptedAsync();
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

        private bool IsVaultFilterMyVault => _vaultFilterSelection == AppResources.MyVault;

        private bool IsVaultFilterOrgVault => _vaultFilterSelection != AppResources.AllVaults &&
                                              _vaultFilterSelection != AppResources.MyVault;

        private string GetVaultFilterOrgId()
        {
            return _organizations?.FirstOrDefault(o => o.Name == _vaultFilterSelection)?.Id;
        }

        private async void CipherOptionsAsync(CipherView cipher)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                await Utilities.AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
            }
        }
    }
}
