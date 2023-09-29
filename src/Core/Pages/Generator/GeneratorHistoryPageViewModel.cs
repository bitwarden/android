using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public class GeneratorHistoryPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IClipboardService _clipboardService;
        private readonly ILogger _logger;

        private bool _showNoData;

        public GeneratorHistoryPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>("passwordGenerationService");
            _clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

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
            Device.BeginInvokeOnMainThread(() =>
            {
                History.ResetWithRange(history ?? new List<GeneratedPasswordHistory>());
                ShowNoData = History.Count == 0;
            });
        }

        public async Task ClearAsync()
        {
            History.ResetWithRange(new List<GeneratedPasswordHistory>());
            ShowNoData = true;
            await _passwordGenerationService.ClearAsync();
        }

        private async void CopyAsync(GeneratedPasswordHistory ph)
        {
            await _clipboardService.CopyTextAsync(ph.Password);
            _platformUtilsService.ShowToastForCopiedValue(AppResources.Password);
        }

        public async Task UpdateOnThemeChanged()
        {
            try
            {
                await Device.InvokeOnMainThreadAsync(() => History.ResetWithRange(new List<GeneratedPasswordHistory>()));

                await InitAsync();
            }
            catch (System.Exception ex)
            {
                _logger.Exception(ex);
            }
        }
    }
}
