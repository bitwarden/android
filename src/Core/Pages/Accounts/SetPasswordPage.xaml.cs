using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Services;

namespace Bit.App.Pages
{
    public partial class SetPasswordPage : BaseContentPage
    {
        private readonly SetPasswordPageViewModel _vm;
        private readonly AppOptions _appOptions;

        public SetPasswordPage(AppOptions appOptions = null, string orgIdentifier = null)
        {
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as SetPasswordPageViewModel;
            _vm.Page = this;
            _vm.SetPasswordSuccessAction =
                () => MainThread.BeginInvokeOnMainThread(async () => await SetPasswordSuccessAsync());
            _vm.CloseAction = async () =>
            {
                await Navigation.PopModalAsync();
            };
            _vm.OrgIdentifier = orgIdentifier;

#if ANDROID
            ToolbarItems.RemoveAt(0);
#endif

            MasterPasswordEntry = _masterPassword;
            ConfirmMasterPasswordEntry = _confirmMasterPassword;

            _masterPassword.ReturnType = ReturnType.Next;
            _masterPassword.ReturnCommand = new Command(() => _confirmMasterPassword.Focus());
            _confirmMasterPassword.ReturnType = ReturnType.Next;
            _confirmMasterPassword.ReturnCommand = new Command(() => _hint.Focus());
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry ConfirmMasterPasswordEntry { get; set; }

        protected override bool ShouldCheckToPreventOnNavigatedToCalledTwice => true;

        protected override async Task InitOnNavigatedToAsync()
        {
            await _vm.InitAsync();
            RequestFocus(_masterPassword);
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task SetPasswordSuccessAsync()
        {
            try
            {
                if (AppHelpers.SetAlternateMainPage(_appOptions))
                {
                    return;
                }
                
                if (_appOptions != null)
                {
                    _appOptions.HasJustLoggedInOrUnlocked = true;
                }
                var previousPage = await AppHelpers.ClearPreviousPage();
                App.MainPage = new TabsPage(_appOptions, previousPage);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }
    }
}
