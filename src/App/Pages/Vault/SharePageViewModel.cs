using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class SharePageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;
        private readonly ICollectionService _collectionService;
        private readonly IOrganizationService _organizationService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private CipherView _cipher;
        private int _organizationSelectedIndex;
        private bool _hasCollections;
        private bool _hasOrganizations;
        private List<Core.Models.View.CollectionView> _writeableCollections;

        public SharePageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            Collections = new ExtendedObservableCollection<CollectionViewModel>();
            OrganizationOptions = new List<KeyValuePair<string, string>>();
            PageTitle = AppResources.MoveToOrganization;

            MoveCommand = new AsyncCommand(MoveAsync, onException: ex => HandleException(ex), allowsMultipleExecutions: false);
        }

        public string CipherId { get; set; }
        public string OrganizationId { get; set; }
        public List<KeyValuePair<string, string>> OrganizationOptions { get; set; }
        public ExtendedObservableCollection<CollectionViewModel> Collections { get; set; }
        public int OrganizationSelectedIndex
        {
            get => _organizationSelectedIndex;
            set
            {
                if (SetProperty(ref _organizationSelectedIndex, value))
                {
                    OrganizationChanged();
                }
            }
        }
        public bool HasCollections
        {
            get => _hasCollections;
            set => SetProperty(ref _hasCollections, value);
        }
        public bool HasOrganizations
        {
            get => _hasOrganizations;
            set => SetProperty(ref _hasOrganizations, value);
        }

        public ICommand MoveCommand { get; }

        public async Task LoadAsync()
        {
            var allCollections = await _collectionService.GetAllDecryptedAsync();
            _writeableCollections = allCollections.Where(c => !c.ReadOnly).ToList();

            var orgs = await _organizationService.GetAllAsync();
            OrganizationOptions = orgs.OrderBy(o => o.Name)
                .Where(o => o.Enabled && o.Status == OrganizationUserStatusType.Confirmed)
                .Select(o => new KeyValuePair<string, string>(o.Name, o.Id)).ToList();
            HasOrganizations = OrganizationOptions.Any();

            var cipherDomain = await _cipherService.GetAsync(CipherId);
            _cipher = await cipherDomain.DecryptAsync();
            if (OrganizationId == null && OrganizationOptions.Any())
            {
                OrganizationId = OrganizationOptions.First().Value;
            }
            OrganizationSelectedIndex = string.IsNullOrWhiteSpace(OrganizationId) ? 0 :
                OrganizationOptions.FindIndex(k => k.Value == OrganizationId);
            FilterCollections();
        }

        public async Task<bool> MoveAsync()
        {
            var selectedCollectionIds = Collections?.Where(c => c.Checked).Select(c => c.Collection.Id);
            if (!selectedCollectionIds?.Any() ?? true)
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.SelectOneCollection,
                    AppResources.Ok);
                return false;
            }
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }

            var cipherDomain = await _cipherService.GetAsync(CipherId);
            var cipherView = await cipherDomain.DecryptAsync();

            var checkedCollectionIds = new HashSet<string>(selectedCollectionIds);
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                await _cipherService.ShareWithServerAsync(cipherView, OrganizationId, checkedCollectionIds);
                await _deviceActionService.HideLoadingAsync();

                var movedItemToOrgText = string.Format(AppResources.MovedItemToOrg, cipherView.Name,
                   (await _organizationService.GetAsync(OrganizationId)).Name);
                _platformUtilsService.ShowToast("success", null, movedItemToOrgText);
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
            catch (System.Exception e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e.Message != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Message, AppResources.AnErrorHasOccurred);
                }
            }
            return false;
        }

        private void OrganizationChanged()
        {
            if (OrganizationSelectedIndex > -1)
            {
                OrganizationId = OrganizationOptions[OrganizationSelectedIndex].Value;
                FilterCollections();
            }
        }

        private void FilterCollections()
        {
            if (OrganizationId == null || !_writeableCollections.Any())
            {
                Collections.ResetWithRange(new List<CollectionViewModel>());
            }
            else
            {
                var cols = _writeableCollections.Where(c => c.OrganizationId == OrganizationId)
                    .Select(c => new CollectionViewModel { Collection = c }).ToList();
                Collections.ResetWithRange(cols);
            }
            HasCollections = Collections.Any();
        }
    }
}
