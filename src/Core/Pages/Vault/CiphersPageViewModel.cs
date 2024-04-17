using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;

namespace Bit.App.Pages
{
    public class CiphersPageViewModel : VaultFilterViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICipherService _cipherService;
        private readonly ISearchService _searchService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAutofillHandler _autofillHandler;
        private readonly IStateService _stateService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IOrganizationService _organizationService;
        private readonly IPolicyService _policyService;
#if ANDROID
        private readonly LazyResolve<IFido2MakeCredentialConfirmationUserInterface> _fido2MakeCredentialConfirmationUserInterface = new LazyResolve<IFido2MakeCredentialConfirmationUserInterface>();
#endif

        private CancellationTokenSource _searchCancellationTokenSource;
        private readonly ILogger _logger;

        private bool _showNoData;
        private bool _showList;
        private bool _websiteIconsEnabled;
        private AppOptions _appOptions;

        public CiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            Ciphers = new ExtendedObservableCollection<CipherItemViewModel>();
            CipherOptionsCommand = CreateDefaultAsyncRelayCommand<CipherView>(cipher => Utilities.AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            AddFabCipherCommand = CreateDefaultAsyncRelayCommand(AddCipherAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            AddCipherCommand = CreateDefaultAsyncRelayCommand(AddCipherAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand CipherOptionsCommand { get; }
        public ICommand AddCipherCommand { get; }
        public ICommand AddFabCipherCommand { get; }
        public ExtendedObservableCollection<CipherItemViewModel> Ciphers { get; set; }
        public Func<CipherView, bool> Filter { get; set; }
        public string AutofillUrl { get; set; }
        public bool Deleted { get; set; }
        public bool ShowAllIfSearchTextEmpty { get; set; }

        protected override ICipherService cipherService => _cipherService;
        protected override IPolicyService policyService => _policyService;
        protected override IOrganizationService organizationService => _organizationService;
        protected override ILogger logger => _logger;

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value, additionalPropertyNames: new string[]
            {
                nameof(ShowSearchDirection),
                nameof(ShowAddCipher)
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

        public bool ShowSearchDirection => !ShowList && !ShowNoData;

        public bool ShowAddCipher => ShowNoData && _appOptions?.OtpData != null;

        internal void Prepare(Func<CipherView, bool> filter, bool deleted, AppOptions appOptions)
        {
            Filter = filter;
            AutofillUrl = appOptions?.Uri;
            Deleted = deleted;
            ShowAllIfSearchTextEmpty = appOptions?.OtpData != null;
            _appOptions = appOptions;
        }

        public async Task InitAsync()
        {
            await InitVaultFilterAsync(true);
            _websiteIconsEnabled = await _stateService.GetDisableFaviconAsync() != true;
            PerformSearchIfPopulated();
        }

        public void Search(string searchText, int? timeout = null)
        {
            var previousCts = _searchCancellationTokenSource;
            var cts = new CancellationTokenSource();
            Task.Run(async () =>
            {
                try
                {
                    List<CipherView> ciphers = null;
                    var searchable = !string.IsNullOrWhiteSpace(searchText) && searchText.Length > 1;
                    var shouldShowAllWhenEmpty = ShowAllIfSearchTextEmpty && string.IsNullOrEmpty(searchText);
                    if (searchable || shouldShowAllWhenEmpty)
                    {
                        if (timeout != null)
                        {
                            await Task.Delay(timeout.Value);
                        }
                        if (searchText != (Page as CiphersPage).SearchBar.Text
                            &&
                            !shouldShowAllWhenEmpty)
                        {
                            return;
                        }

                        previousCts?.Cancel();
                        try
                        {
                            var vaultFilteredCiphers = await GetAllCiphersAsync();
                            if (!shouldShowAllWhenEmpty)
                            {
                                ciphers = await _searchService.SearchCiphersAsync(searchText,
                                    Filter ?? (c => c.IsDeleted == Deleted), vaultFilteredCiphers, cts.Token);
                            }
                            else
                            {
                                ciphers = vaultFilteredCiphers;
                            }
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
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        Ciphers.ResetWithRange(ciphers.Select(c => new CipherItemViewModel(c, _websiteIconsEnabled)).ToList());
                        ShowNoData = !shouldShowAllWhenEmpty && searchable && Ciphers.Count == 0;
                        ShowList = (searchable || shouldShowAllWhenEmpty) && !ShowNoData;
                    });
                }
                catch (Exception ex)
                {
                    _logger.Exception(ex);
                }
            }, cts.Token);
            _searchCancellationTokenSource = cts;
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
#if ANDROID
            if (_fido2MakeCredentialConfirmationUserInterface.Value.IsConfirmingNewCredential)
            {
                await _fido2MakeCredentialConfirmationUserInterface.Value.ConfirmAsync(cipher.Id, cipher.Login.HasFido2Credentials, null);
                return;
            }
#endif

            string selection = null;

            if (!string.IsNullOrWhiteSpace(AutofillUrl))
            {
                var options = new List<string> { AppResources.Autofill };
                if (cipher.Type == CipherType.Login &&
                    Microsoft.Maui.Networking.Connectivity.NetworkAccess != Microsoft.Maui.Networking.NetworkAccess.None)
                {
                    options.Add(AppResources.AutofillAndSave);
                }
                options.Add(AppResources.View);
                selection = await Page.DisplayActionSheet(AppResources.AutofillOrView, AppResources.Cancel, null,
                    options.ToArray());
            }

            if (_appOptions?.OtpData != null)
            {
                if (!await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
                {
                    return;
                }

                var editCipherPage = new CipherAddEditPage(cipher.Id, appOptions: _appOptions);
                await Page.Navigation.PushModalAsync(new NavigationPage(editCipherPage));
                return;
            }

            if (selection == AppResources.View || string.IsNullOrWhiteSpace(AutofillUrl))
            {
                var page = new CipherDetailsPage(cipher.Id);
                await Page.Navigation.PushModalAsync(new NavigationPage(page));
            }
            else if (selection == AppResources.Autofill || selection == AppResources.AutofillAndSave)
            {
                if (!await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
                {
                    return;
                }

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
                    _autofillHandler.Autofill(cipher);
                }
            }
        }

        private void PerformSearchIfPopulated()
        {
            if (!string.IsNullOrWhiteSpace((Page as CiphersPage).SearchBar.Text) || ShowAllIfSearchTextEmpty)
            {
                Search((Page as CiphersPage).SearchBar.Text, 200);
            }
        }

        protected override async Task OnVaultFilterSelectedAsync()
        {
            PerformSearchIfPopulated();
        }

        private async Task AddCipherAsync()
        {
            var pageForLogin = new CipherAddEditPage(null, CipherType.Login, name: _appOptions?.OtpData?.Issuer ?? _appOptions?.OtpData?.AccountName, appOptions: _appOptions);
            await Page.Navigation.PushModalAsync(new NavigationPage(pageForLogin));
        }
    }
}
