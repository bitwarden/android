using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public abstract class BaseCipherViewModel : BaseViewModel
    {
        private readonly IAuditService _auditService;
        protected readonly IDeviceActionService _deviceActionService;
        protected readonly IFileService _fileService;
        protected readonly ILogger _logger;
        protected readonly IPlatformUtilsService _platformUtilsService;
        private CipherView _cipher;
        protected abstract string[] AdditionalPropertiesToRaiseOnCipherChanged { get; }

        public BaseCipherViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _fileService = ServiceContainer.Resolve<IFileService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _auditService = ServiceContainer.Resolve<IAuditService>("auditService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            CheckPasswordCommand = new AsyncCommand(CheckPasswordAsync, allowsMultipleExecutions: false);
        }

        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value, additionalPropertyNames: AdditionalPropertiesToRaiseOnCipherChanged);
        }

        public string CreationDate => string.Format(AppResources.CreatedXY, Cipher.CreationDate.ToShortDateString(), Cipher.CreationDate.ToShortTimeString());

        public AsyncCommand CheckPasswordCommand { get; }

        protected async Task CheckPasswordAsync()
        {
            try
            {
                if (string.IsNullOrWhiteSpace(Cipher?.Login?.Password))
                {
                    return;
                }

                await _deviceActionService.ShowLoadingAsync(AppResources.CheckingPassword);
                var matches = await _auditService.PasswordLeakedAsync(Cipher.Login.Password);
                await _deviceActionService.HideLoadingAsync();

                await _platformUtilsService.ShowDialogAsync(matches > 0
                    ? string.Format(AppResources.PasswordExposed, matches.ToString("N0"))
                    : AppResources.PasswordSafe);
            }
            catch (ApiException apiException)
            {
                _logger.Exception(apiException);
                await _deviceActionService.HideLoadingAsync();
                if (apiException?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(apiException.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.AnErrorHasOccurred);
            }
        }
    }
}
