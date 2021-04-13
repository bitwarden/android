using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
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
        private CancellationTokenSource _searchCancellationTokenSource;

        private bool _showNoData;
        private bool _showList;
        private bool _websiteIconsEnabled;

        public CiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");

            Ciphers = new ExtendedObservableCollection<CipherView>();
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
        }

        public Command CipherOptionsCommand { get; set; }
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

        public bool ShowSearchDirection => !ShowList && !ShowNoData;

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public async Task InitAsync()
        {
            WebsiteIconsEnabled = !(await _stateService.GetAsync<bool?>(Constants.DisableFaviconKey))
                .GetValueOrDefault();
            if (!string.IsNullOrWhiteSpace((Page as CiphersPage).SearchBar.Text))
            {
                Search((Page as CiphersPage).SearchBar.Text, 200);
            }
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
                        ciphers = await _searchService.SearchCiphersAsync(searchText, 
                            Filter ?? (c => c.IsDeleted == Deleted), null, cts.Token);
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

        private async void CipherOptionsAsync(CipherView cipher)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                await Utilities.AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
            }
        }
    }
}
