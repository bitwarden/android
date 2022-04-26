using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class PasswordHistoryPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICipherService _cipherService;
        private readonly IClipboardService _clipboardService;

        private bool _showNoData;

        public PasswordHistoryPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");

            PageTitle = AppResources.PasswordHistory;
            History = new ExtendedObservableCollection<PasswordHistoryView>();
            CopyCommand = new Command<PasswordHistoryView>(CopyAsync);
        }

        public Command CopyCommand { get; set; }
        public string CipherId { get; set; }
        public ExtendedObservableCollection<PasswordHistoryView> History { get; set; }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }

        public async Task InitAsync()
        {
            var cipher = await _cipherService.GetAsync(CipherId);
            var decCipher = await cipher.DecryptAsync();
            History.ResetWithRange(decCipher.PasswordHistory ?? new List<PasswordHistoryView>());
            ShowNoData = History.Count == 0;
        }

        private async void CopyAsync(PasswordHistoryView ph)
        {
            await _clipboardService.CopyTextAsync(ph.Password);
            _platformUtilsService.ShowToast("info", null,
                string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
        }
    }
}
