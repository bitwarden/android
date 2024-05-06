using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class DeleteAccountViewModel : BaseViewModel
    {
        readonly IPlatformUtilsService _platformUtilsService;
        readonly IVerificationActionsFlowHelper _verificationActionsFlowHelper;
        readonly ILogger _logger;

        public DeleteAccountViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _verificationActionsFlowHelper = ServiceContainer.Resolve<IVerificationActionsFlowHelper>("verificationActionsFlowHelper");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.DeleteAccount;
        }

        public async Task DeleteAccountAsync()
        {
            try
            {
                if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                        AppResources.InternetConnectionRequiredTitle, AppResources.Ok);
                    return;
                }

                await _verificationActionsFlowHelper
                    .Configure(VerificationFlowAction.DeleteAccount,
                               null,
                               AppResources.DeleteAccount,
                               true)
                    .ValidateAndExecuteAsync();
            }
            catch (System.Exception ex)
            {
                _logger.Exception(ex);
                await _platformUtilsService.ShowDialogAsync(AppResources.AnErrorHasOccurred);
            }
        }
    }

    public interface IDeleteAccountActionFlowExecutioner : IActionFlowExecutioner { }

    public class DeleteAccountActionFlowExecutioner : IDeleteAccountActionFlowExecutioner
    {
        readonly IApiService _apiService;
        readonly IMessagingService _messagingService;
        readonly IPlatformUtilsService _platformUtilsService;
        readonly IDeviceActionService _deviceActionService;
        readonly ILogger _logger;

        public DeleteAccountActionFlowExecutioner(IApiService apiService,
            IMessagingService messagingService,
            IPlatformUtilsService platformUtilsService,
            IDeviceActionService deviceActionService,
            ILogger logger)
        {
            _apiService = apiService;
            _messagingService = messagingService;
            _platformUtilsService = platformUtilsService;
            _deviceActionService = deviceActionService;
            _logger = logger;
        }

        public async Task Execute(IActionFlowParmeters parameters)
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.DeletingYourAccount);

                await _apiService.DeleteAccountAsync(new Core.Models.Request.DeleteAccountRequest
                {
                    MasterPasswordHash = parameters.VerificationType == Core.Enums.VerificationType.MasterPassword ? parameters.Secret : (string)null,
                    OTP = parameters.VerificationType == Core.Enums.VerificationType.OTP ? parameters.Secret : (string)null
                });

                await _deviceActionService.HideLoadingAsync();

                _messagingService.Send("logout");

                await _platformUtilsService.ShowDialogAsync(AppResources.YourAccountHasBeenPermanentlyDeleted);
            }
            catch (ApiException apiEx)
            {
                await _deviceActionService.HideLoadingAsync();

                if (apiEx?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(apiEx.Error.GetSingleMessage(), AppResources.AnErrorHasOccurred);
                }
            }
            catch (System.Exception ex)
            {
                await _deviceActionService.HideLoadingAsync();
                _logger.Exception(ex);
                await _platformUtilsService.ShowDialogAsync(AppResources.AnErrorHasOccurred);
            }
        }
    }
}
