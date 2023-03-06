using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AutofillCiphersPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAutofillHandler _autofillHandler;
        private readonly ICipherService _cipherService;
        private readonly IStateService _stateService;
        private readonly ISyncService _syncService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IMessagingService _messagingService;
        private readonly ILogger _logger;

        private bool _refreshing;
        private bool _showNoData;
        private bool _showList;
        private string _noDataText;
        private bool _websiteIconsEnabled;

        public AutofillCiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            GroupedItems = new ObservableRangeCollection<IGroupingsPageListItem>();
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
            RefreshCommand = new Command(RefreshAsync);

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowAddAccountRow = false
            };
        }

        public string Name { get; set; }
        public string Uri { get; set; }
        public Command CipherOptionsCommand { get; set; }
        public Command RefreshCommand { get; set; }
        public bool LoadedOnce { get; set; }
        public ObservableRangeCollection<IGroupingsPageListItem> GroupedItems { get; set; }
        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }

        public bool Refreshing
        {
            get => _refreshing;
            set => SetProperty(ref _refreshing, value);
        }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }

        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value);
        }

        public string NoDataText
        {
            get => _noDataText;
            set => SetProperty(ref _noDataText, value);
        }
        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public void Init(AppOptions appOptions)
        {
            Uri = appOptions?.Uri;
            string name = null;
            if (Uri?.StartsWith(Constants.AndroidAppProtocol) ?? false)
            {
                name = Uri.Substring(Constants.AndroidAppProtocol.Length);
            }
            else
            {
                name = CoreHelpers.GetDomain(Uri);
            }
            if (string.IsNullOrWhiteSpace(name))
            {
                name = "--";
            }
            Name = name;
            PageTitle = string.Format(AppResources.ItemsForUri, Name ?? "--");
            NoDataText = string.Format(AppResources.NoItemsForUri, Name ?? "--");
        }

        public async Task LoadAsync()
        {
            LoadedOnce = true;
            ShowList = false;
            ShowNoData = false;
            WebsiteIconsEnabled = !(await _stateService.GetDisableFaviconAsync()).GetValueOrDefault();
            var groupedItems = new List<GroupingsPageListGroup>();
            var ciphers = await _cipherService.GetAllDecryptedByUrlAsync(Uri, null);
            var matching = ciphers.Item1?.Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
            var hasMatching = matching?.Any() ?? false;
            if (matching?.Any() ?? false)
            {
                groupedItems.Add(
                    new GroupingsPageListGroup(matching, AppResources.MatchingItems, matching.Count, false, true));
            }
            var fuzzy = ciphers.Item2?.Select(c =>
                new GroupingsPageListItem { Cipher = c, FuzzyAutofill = true }).ToList();
            if (fuzzy?.Any() ?? false)
            {
                groupedItems.Add(
                    new GroupingsPageListGroup(fuzzy, AppResources.PossibleMatchingItems, fuzzy.Count, false,
                    !hasMatching));
            }

            // TODO: refactor this
            if (Device.RuntimePlatform == Device.Android
                ||
                GroupedItems.Any())
            {
                var items = new List<IGroupingsPageListItem>();
                foreach (var itemGroup in groupedItems)
                {
                    items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                    items.AddRange(itemGroup);
                }

                GroupedItems.ReplaceRange(items);
            }
            else
            {
                // HACK: we need this on iOS, so that it doesn't crash when adding coming from an empty list
                var first = true;
                var items = new List<IGroupingsPageListItem>();
                foreach (var itemGroup in groupedItems)
                {
                    if (!first)
                    {
                        items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                    }
                    else
                    {
                        first = false;
                    }
                    items.AddRange(itemGroup);
                }

                if (groupedItems.Any())
                {
                    GroupedItems.ReplaceRange(new List<IGroupingsPageListItem> { new GroupingsPageHeaderListItem(groupedItems[0].Name, groupedItems[0].ItemCount) });
                    GroupedItems.AddRange(items);
                }
                else
                {
                    GroupedItems.Clear();
                }
            }
            ShowList = groupedItems.Any();
            ShowNoData = !ShowList;
        }

        public async Task SelectCipherAsync(CipherView cipher, bool fuzzy)
        {
            if (cipher == null)
            {
                return;
            }
            if (_deviceActionService.SystemMajorVersion() < 21)
            {
                await AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
            }
            else
            {
                if (cipher.Reprompt != CipherRepromptType.None && !await _passwordRepromptService.ShowPasswordPromptAsync())
                {
                    return;
                }
                var autofillResponse = AppResources.Yes;
                if (fuzzy)
                {
                    var options = new List<string> { AppResources.Yes };
                    if (cipher.Type == CipherType.Login &&
                        Xamarin.Essentials.Connectivity.NetworkAccess != Xamarin.Essentials.NetworkAccess.None)
                    {
                        options.Add(AppResources.YesAndSave);
                    }
                    autofillResponse = await _deviceActionService.DisplayAlertAsync(null,
                        string.Format(AppResources.BitwardenAutofillServiceMatchConfirm, Name), AppResources.No,
                        options.ToArray());
                }
                if (autofillResponse == AppResources.YesAndSave && cipher.Type == CipherType.Login)
                {
                    var uris = cipher.Login?.Uris?.ToList();
                    if (uris == null)
                    {
                        uris = new List<LoginUriView>();
                    }
                    uris.Add(new LoginUriView
                    {
                        Uri = Uri,
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
                if (autofillResponse == AppResources.Yes || autofillResponse == AppResources.YesAndSave)
                {
                    _autofillHandler.Autofill(cipher);
                }
            }
        }

        private async void CipherOptionsAsync(CipherView cipher)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                await AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService);
            }
        }

        public async void RefreshAsync()
        {
            Refreshing = true;
            try
            {
                await Task.Delay(500);
                await _syncService.FullSyncAsync(false);
            }
            finally
            {
                Refreshing = false;
            }
        }
    }
}
