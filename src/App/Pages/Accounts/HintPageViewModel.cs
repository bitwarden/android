using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class HintPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IApiService _apiService;
        private readonly ILogger _logger;
        private string _email;

        public HintPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _logger = ServiceContainer.Resolve<ILogger>();

            PageTitle = AppResources.PasswordHint;
            SubmitCommand = new AsyncCommand(SubmitAsync,
                onException: ex =>
                {
                    _logger.Exception(ex);
                    _deviceActionService.DisplayAlertAsync(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok).FireAndForget();
                },
                allowsMultipleExecutions: false);
        }

        public ICommand SubmitCommand { get; }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value);
        }

        public async Task SubmitAsync()
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            if (string.IsNullOrWhiteSpace(Email))
            {
                await _deviceActionService.DisplayAlertAsync(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.Ok);
                return;
            }
            if (!Email.Contains("@"))
            {
                await _deviceActionService.DisplayAlertAsync(AppResources.AnErrorHasOccurred, AppResources.InvalidEmail, AppResources.Ok);
                return;
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Submitting);
                await _apiService.PostPasswordHintAsync(
                    new Core.Models.Request.PasswordHintRequest { Email = Email });
                await _deviceActionService.HideLoadingAsync();
                await _deviceActionService.DisplayAlertAsync(null, AppResources.PasswordHintAlert, AppResources.Ok);
                await Page.Navigation.PopModalAsync();
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
        }
    }
}
