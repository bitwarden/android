using Bit.App.Abstractions;
using Bit.App.Models;
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
    public class AutofillCiphersPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;

        private AppOptions _appOptions;
        private string _name;
        private string _uri;
        private bool _showNoData;
        private bool _showList;

        public AutofillCiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");

            GroupedItems = new ExtendedObservableCollection<GroupingsPageListGroup>();
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
        }

        public Command CipherOptionsCommand { get; set; }
        public ExtendedObservableCollection<GroupingsPageListGroup> GroupedItems { get; set; }

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

        public void Init(AppOptions appOptions)
        {
            _appOptions = appOptions;
            _uri = appOptions.Uri;
            if(_uri.StartsWith(Constants.AndroidAppProtocol))
            {
                _name = _uri.Substring(Constants.AndroidAppProtocol.Length);
            }
            else if(!Uri.TryCreate(_uri, UriKind.Absolute, out Uri uri) ||
                !DomainName.TryParseBaseDomain(uri.Host, out _name))
            {
                _name = "--";
            }
            PageTitle = string.Format(AppResources.ItemsForUri, _name ?? "--");
        }

        public async Task LoadAsync()
        {
            ShowNoData = false;
            ShowList = false;

            var ciphers = await _cipherService.GetAllDecryptedByUrlAsync(_uri, null);
            var matching = ciphers.Item1?.Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
            var matchingGroup = new GroupingsPageListGroup(matching, AppResources.MatchingItems, matching.Count, false);
            var fuzzy = ciphers.Item2?.Select(c => new GroupingsPageListItem { Cipher = c, FuzzyAutofill = true })
                .ToList();
            var fuzzyGroup = new GroupingsPageListGroup(fuzzy, AppResources.PossibleMatchingItems, fuzzy.Count, false);
            GroupedItems.ResetWithRange(new List<GroupingsPageListGroup> { matchingGroup, fuzzyGroup });

            ShowNoData = !matching.Any() && !fuzzy.Any();
            ShowList = !ShowNoData;
        }

        public async Task SelectCipherAsync(CipherView cipher, bool fuzzy)
        {
            if(_deviceActionService.SystemMajorVersion() < 21)
            {
                // TODO
            }
            else
            {
                var autofillResponse = AppResources.Yes;
                if(fuzzy)
                {
                    var options = new List<string> { AppResources.Yes };
                    if(cipher.Type == CipherType.Login)
                    {
                        options.Add(AppResources.YesAndSave);
                    }
                    autofillResponse = await _deviceActionService.DisplayAlertAsync(null,
                        string.Format(AppResources.BitwardenAutofillServiceMatchConfirm, _name), AppResources.No,
                        options.ToArray());
                }
                if(autofillResponse == AppResources.YesAndSave && cipher.Type == CipherType.Login)
                {
                    var uris = cipher.Login?.Uris?.ToList();
                    if(uris == null)
                    {
                        uris = new List<LoginUriView>();
                    }
                    uris.Add(new LoginUriView
                    {
                        Uri = _uri,
                        Match = null
                    });
                    cipher.Login.Uris = uris;
                    try
                    {
                        await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                        await _cipherService.SaveWithServerAsync(await _cipherService.EncryptAsync(cipher));
                        await _deviceActionService.HideLoadingAsync();
                    }
                    catch(ApiException e)
                    {
                        await _deviceActionService.HideLoadingAsync();
                        await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(),
                            AppResources.Ok);
                    }
                }
                if(autofillResponse == AppResources.Yes || autofillResponse == AppResources.YesAndSave)
                {
                    _deviceActionService.Autofill(cipher);
                }
            }
        }

        private async void CipherOptionsAsync(CipherView cipher)
        {
            if(!(Page as BaseContentPage).DoOnce())
            {
                return;
            }
            var option = await Page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, "1", "2");
            if(option == AppResources.Cancel)
            {
                return;
            }
            // TODO: process options
        }
    }
}
