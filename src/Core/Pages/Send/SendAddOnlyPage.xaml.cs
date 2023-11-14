using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    /// <summary>
    /// This is a version of <see cref="SendAddEditPage"/> that is reduced for adding only and adapted
    /// for performance for iOS Share extension.
    /// </summary>
    /// <remarks>
    /// This should NOT be used in Android.
    /// </remarks>
    public partial class SendAddOnlyPage : BaseContentPage
    {
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        private AppOptions _appOptions;
        private SendAddEditPageViewModel _vm;

        public Action OnClose { get; set; }
        public Action AfterSubmit { get; set; }

        public SendAddOnlyPage(
            AppOptions appOptions = null,
            string sendId = null,
            SendType? type = null)
        {
            if (appOptions?.IosExtension != true)
            {
                throw new InvalidOperationException(nameof(SendAddOnlyPage) + " is only prepared to be used in iOS share extension");
            }

            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as SendAddEditPageViewModel;
            _vm.Page = this;
            _vm.SendId = sendId;
            _vm.Type = appOptions?.CreateSend?.Item1 ?? type;

            if (_vm.IsText)
            {
                _nameEntry.ReturnType = ReturnType.Next;
                _nameEntry.ReturnCommand = new Command(() => _textEditor.Focus());
            }
        }

        protected override async void OnNavigatedTo(NavigatedToEventArgs args)
        {
            try
            {
                if (!await AppHelpers.IsVaultTimeoutImmediateAsync())
                {
                    await _vaultTimeoutService.CheckVaultTimeoutAsync();
                }
                if (await _vaultTimeoutService.IsLockedAsync())
                {
                    return;
                }

                await _vm.InitAsync();

                if (!await _vm.LoadAsync())
                {
                    await CloseAsync();
                    return;
                }

                _accountAvatar?.OnAppearing();
                await MainThread.InvokeOnMainThreadAsync(async () => _vm.AvatarImageSource = await GetAvatarImageSourceAsync());

                await HandleCreateRequest();
                if (string.IsNullOrWhiteSpace(_vm.Send?.Name))
                {
                    RequestFocus(_nameEntry);
                }
                AdjustToolbar();
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
                await CloseAsync();
            }
        }

        protected override void OnNavigatingFrom(NavigatingFromEventArgs args)
        {
            base.OnNavigatingFrom(args);

            _accountAvatar?.OnDisappearing();
        }

        private async Task CloseAsync()
        {
            if (OnClose is null)
            {
                await Navigation.PopModalAsync();
            }
            else
            {
                OnClose();
            }
        }

        private async void Save_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var submitted = await _vm.SubmitAsync();
                if (submitted)
                {
                    AfterSubmit?.Invoke();
                }
            }
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await CloseAsync();
            }
        }

        private void AdjustToolbar()
        {
            _saveItem.IsEnabled = _vm.SendEnabled;
        }

        private Task HandleCreateRequest()
        {
            if (_appOptions?.CreateSend == null)
            {
                return Task.CompletedTask;
            }

            _vm.IsAddFromShare = true;
            _vm.CopyInsteadOfShareAfterSaving = _appOptions.CopyInsteadOfShareAfterSaving;

            var name = _appOptions.CreateSend.Item2;
            _vm.Send.Name = name;

            var type = _appOptions.CreateSend.Item1;
            if (type == SendType.File)
            {
                _vm.FileData = _appOptions.CreateSend.Item3;
                _vm.FileName = name;
            }
            else
            {
                var text = _appOptions.CreateSend.Item4;
                _vm.Send.Text.Text = text;
                _vm.TriggerSendTextPropertyChanged();
            }
            _appOptions.CreateSend = null;

            return Task.CompletedTask;
        }

        async void OptionsHeader_Tapped(object sender, EventArgs e)
        {
            try
            {
                _vm.ToggleOptionsCommand.Execute(null);

                if (!_lazyOptionsView.HasLazyViewLoaded)
                {
                    _lazyOptionsView.MainScrollView = _scrollView;
                    await _lazyOptionsView.LoadViewAsync();
                }
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }
    }
}
