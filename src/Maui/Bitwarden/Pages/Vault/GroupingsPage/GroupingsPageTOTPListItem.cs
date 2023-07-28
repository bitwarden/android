using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.App.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public class GroupingsPageTOTPListItem : ExtendedViewModel, IGroupingsPageListItem
    {
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");
        private readonly ITotpService _totpService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IClipboardService _clipboardService;
        private CipherView _cipher;

        private bool _websiteIconsEnabled;
        private string _iconImageSource = string.Empty;

        private double _progress;
        private string _totpSec;
        private string _totpCodeFormatted;
        private TotpHelper _totpTickHelper;


        public GroupingsPageTOTPListItem(CipherView cipherView, bool websiteIconsEnabled)
        {
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");

            Cipher = cipherView;
            WebsiteIconsEnabled = websiteIconsEnabled;
            CopyCommand = new AsyncCommand(CopyToClipboardAsync,
                 onException: ex => _logger.Value.Exception(ex),
                 allowsMultipleExecutions: false);
            _totpTickHelper = new TotpHelper(cipherView);
        }

        public AsyncCommand CopyCommand { get; set; }

        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }

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
        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public bool ShowIconImage
        {
            get => WebsiteIconsEnabled
                && !string.IsNullOrWhiteSpace(Cipher.Login?.Uri)
                && IconImageSource != null;
        }

        public string IconImageSource
        {
            get
            {
                if (_iconImageSource == string.Empty) // default value since icon source can return null
                {
                    _iconImageSource = IconImageHelper.GetLoginIconImage(Cipher);
                }
                return _iconImageSource;
            }

        }

        public string TotpCodeFormattedStart => TotpCodeFormatted?.Split(' ')[0];

        public string TotpCodeFormattedEnd => TotpCodeFormatted?.Split(' ')[1];

        public async Task CopyToClipboardAsync()
        {
            await _clipboardService.CopyTextAsync(TotpCodeFormatted?.Replace(" ", string.Empty));
            _platformUtilsService.ShowToast("info", null, string.Format(AppResources.ValueHasBeenCopied, AppResources.VerificationCodeTotp));
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
