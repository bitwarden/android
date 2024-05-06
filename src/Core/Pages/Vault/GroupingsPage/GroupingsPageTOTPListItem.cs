using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;
using CommunityToolkit.Mvvm.Input;

namespace Bit.App.Pages
{
    public class GroupingsPageTOTPListItem : CipherItemViewModel, IGroupingsPageListItem
    {
        private readonly IClipboardService _clipboardService;

        private double _progress;
        private string _totpSec;
        private string _totpCodeFormatted;
        private readonly TotpHelper _totpTickHelper;

        public GroupingsPageTOTPListItem(CipherView cipherView, bool websiteIconsEnabled)
            :base(cipherView, websiteIconsEnabled)
        {
            _clipboardService = ServiceContainer.Resolve<IClipboardService>();

            CopyCommand = CreateDefaultAsyncRelayCommand(CopyToClipboardAsync,
                 onException: _logger.Value.Exception,
                 allowsMultipleExecutions: false);
            _totpTickHelper = new TotpHelper(cipherView);
        }

        public AsyncRelayCommand CopyCommand { get; set; }

        public string TotpCodeFormatted
        {
            get => _totpCodeFormatted;
            set => SetProperty(ref _totpCodeFormatted, value,
                additionalPropertyNames: new string[]
                {
                    nameof(TotpCodeFormattedStart),
                    nameof(TotpCodeFormattedEnd),
                });
        }

        public string TotpSec
        {
            get => _totpSec;
            set => SetProperty(ref _totpSec, value);
        }
        public double Progress
        {
            get => _progress;
            set => SetProperty(ref _progress, value);
        }

        public string TotpCodeFormattedStart => TotpCodeFormatted?.Split(' ')[0];

        public string TotpCodeFormattedEnd => TotpCodeFormatted?.Split(' ')[1];

        public async Task CopyToClipboardAsync()
        {
            await _clipboardService.CopyTextAsync(TotpCodeFormatted?.Replace(" ", string.Empty));
            _platformUtilsService.Value.ShowToast("info", null, string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
        }

        public async Task TotpTickAsync()
        {
            await _totpTickHelper.GenerateNewTotpValues();
            MainThread.BeginInvokeOnMainThread(() =>
            {
                TotpSec = _totpTickHelper.TotpSec;
                Progress = _totpTickHelper.Progress;
                TotpCodeFormatted = _totpTickHelper.TotpCodeFormatted;
            });
        }
    }
}
