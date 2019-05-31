using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class HintPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IApiService _apiService;

        public HintPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");

            PageTitle = AppResources.PasswordHint;
            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public Command SubmitCommand { get; }
        public string Email { get; set; }

        public async Task SubmitAsync()
        {
            if(string.IsNullOrWhiteSpace(Email))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.Ok);
                return;
            }
            if(!Email.Contains("@"))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.InvalidEmail, AppResources.Ok);
                return;
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Submitting);
                await _apiService.PostPasswordHintAsync(
                    new Core.Models.Request.PasswordHintRequest { Email = Email });
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(null, AppResources.PasswordHintAlert, AppResources.Ok);
                await Page.Navigation.PopModalAsync();
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
        }
    }
}
