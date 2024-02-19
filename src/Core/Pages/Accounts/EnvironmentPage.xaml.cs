using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class EnvironmentPage : BaseContentPage
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly EnvironmentPageViewModel _vm;

        public EnvironmentPage()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            InitializeComponent();
            _vm = BindingContext as EnvironmentPageViewModel;
            _vm.Page = this;

#if ANDROID
            ToolbarItems.RemoveAt(0);
#endif

            _webVaultEntry.ReturnType = ReturnType.Next;
            _webVaultEntry.ReturnCommand = new Command(() => _apiEntry.Focus());
            _apiEntry.ReturnType = ReturnType.Next;
            _apiEntry.ReturnCommand = new Command(() => _identityEntry.Focus());
            _identityEntry.ReturnType = ReturnType.Next;
            _identityEntry.ReturnCommand = new Command(() => _iconsEntry.Focus());
            _vm.SubmitSuccessAction = () => MainThread.BeginInvokeOnMainThread(async () => await SubmitSuccessAsync());
            _vm.CloseAction = async () =>
            {
                await Navigation.PopModalAsync();
            };
        }

        private async Task SubmitSuccessAsync()
        {
            _platformUtilsService.ShowToast("success", null, AppResources.EnvironmentSaved);
            await Navigation.PopModalAsync();
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }
    }
}
