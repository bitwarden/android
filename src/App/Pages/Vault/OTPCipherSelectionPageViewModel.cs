using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class OTPCipherSelectionPageViewModel : CipherSelectionPageViewModel
    {
        private readonly ISearchService _searchService = ServiceContainer.Resolve<ISearchService>();

        private OtpData _otpData;
        private Models.AppOptions _appOptions;

        public override void Init(Models.AppOptions options)
        {
            _appOptions = options;
            _otpData = options.OtpData.Value;

            Name = _otpData.Issuer ?? _otpData.AccountName;
            PageTitle = string.Format(AppResources.ItemsForUri, Name ?? "--");
            NoDataText = string.Format(AppResources.NoItemsForUri, Name ?? "--");
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
                    new GroupingsPageListGroup(ciphers.Select(c => new GroupingsPageListItem { Cipher = c }).ToList(),
                        AppResources.MatchingItems,
                        ciphers.Count,
                        false,
                        true));
            }

            return groupedItems;
        }

        protected override async Task SelectCipherAsync(IGroupingsPageListItem item)
        {
            if (!(item is GroupingsPageListItem listItem) || listItem.Cipher is null)
            {
                return;
            }

            var cipher = listItem.Cipher;

            if (cipher.Reprompt != CipherRepromptType.None && !await _passwordRepromptService.ShowPasswordPromptAsync())
            {
                return;
            }

            var editCipherPage = new CipherAddEditPage(cipher.Id, appOptions: _appOptions);
            await Page.Navigation.PushModalAsync(new NavigationPage(editCipherPage));
            return;
            // Move to details

            cipher.Login.Totp = _otpData.Uri;
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                await _cipherService.SaveWithServerAsync(await _cipherService.EncryptAsync(cipher));
                await _deviceActionService.HideLoadingAsync();

                _platformUtilsService.ShowToast(null, AppResources.AuthenticatorKey, AppResources.AuthenticatorKeyAdded);

                _messagingService.Send(App.POP_ALL_AND_GO_TO_TAB_MYVAULT_MESSAGE);
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
    }
}
