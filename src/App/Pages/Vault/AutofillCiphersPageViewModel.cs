using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
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
        private readonly ICipherService _cipherService;
        private readonly ISearchService _searchService;
        private CancellationTokenSource _searchCancellationTokenSource;

        private AppOptions _appOptions;
        private string _name;
        private string _uri;
        private bool _showNoData;
        private bool _showList;

        public AutofillCiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");

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
            var fuzzy = ciphers.Item2?.Select(c => new GroupingsPageListItem { Cipher = c }).ToList();
            var fuzzyGroup = new GroupingsPageListGroup(fuzzy, AppResources.PossibleMatchingItems, fuzzy.Count, false);
            GroupedItems.ResetWithRange(new List<GroupingsPageListGroup> { matchingGroup, fuzzyGroup });

            ShowNoData = !matching.Any() && !fuzzy.Any();
            ShowList = !ShowNoData;
        }

        public async Task SelectCipherAsync(CipherView cipher)
        {
            // TODO
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
