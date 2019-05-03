using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class CiphersPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICipherService _cipherService;

        private string _searchText;
        private string _noDataText;
        private bool _showNoData;

        public CiphersPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");

            PageTitle = AppResources.SearchVault;
            Ciphers = new ExtendedObservableCollection<CipherView>();
            CipherOptionsCommand = new Command<CipherView>(CipherOptionsAsync);
        }

        public Command CipherOptionsCommand { get; set; }
        public ExtendedObservableCollection<CipherView> Ciphers { get; set; }

        public string SearchText
        {
            get => _searchText;
            set => SetProperty(ref _searchText, value);
        }

        public string NoDataText
        {
            get => _noDataText;
            set => SetProperty(ref _noDataText, value);
        }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }

        public async Task LoadAsync()
        {
            var ciphers = await _cipherService.GetAllDecryptedAsync();
            Ciphers.ResetWithRange(ciphers);
            ShowNoData = Ciphers.Count == 0;
        }

        private async void CipherOptionsAsync(CipherView cipher)
        {
            var option = await Page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, "1", "2");
            if(option == AppResources.Cancel)
            {
                return;
            }
            // TODO: process options
        }
    }
}
