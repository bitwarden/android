using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GeneratorHistoryPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IPasswordGenerationService _passwordGenerationService;

        private bool _showNoData;

        public GeneratorHistoryPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");

            PageTitle = AppResources.PasswordHistory;
            History = new ExtendedObservableCollection<GeneratedPasswordHistory>();
            CopyCommand = new Command<GeneratedPasswordHistory>(CopyAsync);
        }

        public Command CopyCommand { get; set; }
        public ExtendedObservableCollection<GeneratedPasswordHistory> History { get; set; }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value);
        }

        public async Task InitAsync()
        {
            var history = await _passwordGenerationService.GetHistoryAsync();
            History.ResetWithRange(history ?? new List<GeneratedPasswordHistory>());
            ShowNoData = History.Count == 0;
        }

        public async Task ClearAsync()
        {
            History.ResetWithRange(new List<GeneratedPasswordHistory>());
            await _passwordGenerationService.ClearAsync();
        }

        private async void CopyAsync(GeneratedPasswordHistory ph)
        {
            await _platformUtilsService.CopyToClipboardAsync(ph.Password);
            _platformUtilsService.ShowToast("info", null,
                string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
        }
    }
}
