using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class OTPCipherSelectionPageViewModel : CipherSelectionPageViewModel
    {
        private readonly ISearchService _searchService = ServiceContainer.Resolve<ISearchService>();

        private OtpData _otpData;
        private Models.AppOptions _appOptions;

        public override bool ShowCallout => !ShowNoData;

        public override void Init(Models.AppOptions options)
        {
            _appOptions = options;
            _otpData = options.OtpData.Value;

            Name = _otpData.Issuer ?? _otpData.AccountName;
            PageTitle = string.Format(AppResources.ItemsForUri, Name ?? "--");
            NoDataText = string.Format(AppResources.ThereAreNoItemsInYourVaultThatMatchX, Name ?? "--")
                + Environment.NewLine
                + AppResources.SearchForAnItemOrAddANewItem;
        }

        protected override async Task<List<GroupingsPageListGroup>> LoadGroupedItemsAsync()
        {
            var groupedItems = new List<GroupingsPageListGroup>();
            var allCiphers = await _cipherService.GetAllDecryptedAsync();
            var ciphers = await _searchService.SearchCiphersAsync(_otpData.Issuer ?? _otpData.AccountName,
                            c => c.Type == CipherType.Login && !c.IsDeleted, allCiphers);

            if (ciphers?.Any() ?? false)
            {
                groupedItems.Add(
                    new GroupingsPageListGroup(ciphers.Select(c => new CipherItemViewModel(c, WebsiteIconsEnabled)).ToList(),
                        AppResources.MatchingItems,
                        ciphers.Count,
                        false,
                        true));
            }

            return groupedItems;
        }

        protected override async Task SelectCipherAsync(IGroupingsPageListItem item)
        {
            if (!(item is CipherItemViewModel listItem) || listItem.Cipher is null)
            {
                return;
            }

            var cipher = listItem.Cipher;

            if (!await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(cipher.Reprompt))
            {
                return;
            }

            var editCipherPage = new CipherAddEditPage(cipher.Id, appOptions: _appOptions);
            await Page.Navigation.PushModalAsync(new NavigationPage(editCipherPage));
            return;
        }

        protected override async Task AddCipherAsync()
        {
            var pageForLogin = new CipherAddEditPage(null, CipherType.Login, name: Name, appOptions: _appOptions);
            await Page.Navigation.PushModalAsync(new NavigationPage(pageForLogin));
        }

        protected override async Task AddFabCipherAsync()
        {
            await AddCipherAsync();
        }
    }
}
