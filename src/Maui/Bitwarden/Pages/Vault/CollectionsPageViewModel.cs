using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class CollectionsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;
        private readonly ICollectionService _collectionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private CipherView _cipher;
        private Cipher _cipherDomain;
        private bool _hasCollections;

        public CollectionsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            Collections = new ExtendedObservableCollection<CollectionViewModel>();
            PageTitle = AppResources.Collections;
        }

        public string CipherId { get; set; }
        public ExtendedObservableCollection<CollectionViewModel> Collections { get; set; }
        public bool HasCollections
        {
            get => _hasCollections;
            set => SetProperty(ref _hasCollections, value);
        }

        public async Task LoadAsync()
        {
            _cipherDomain = await _cipherService.GetAsync(CipherId);
            var collectionIds = _cipherDomain.CollectionIds;
            _cipher = await _cipherDomain.DecryptAsync();
            var allCollections = await _collectionService.GetAllDecryptedAsync();
            var collections = allCollections
                .Where(c => !c.ReadOnly && c.OrganizationId == _cipher.OrganizationId)
                .Select(c => new CollectionViewModel
                {
                    Collection = c,
                    Checked = collectionIds.Contains(c.Id)
                }).ToList();
            Collections.ResetWithRange(collections);
            HasCollections = Collections.Any();
        }

        public async Task<bool> SubmitAsync()
        {
            var selectedCollectionIds = Collections?.Where(c => c.Checked).Select(c => c.Collection.Id);
            if (!selectedCollectionIds?.Any() ?? true)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.SelectOneCollection,
                    AppResources.Ok);
                return false;
            }
            if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }

            _cipherDomain.CollectionIds = new HashSet<string>(selectedCollectionIds);
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                await _cipherService.SaveCollectionsWithServerAsync(_cipherDomain);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.ItemUpdated);
                await Page.Navigation.PopModalAsync();
                return true;
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
            return false;
        }
    }
}
