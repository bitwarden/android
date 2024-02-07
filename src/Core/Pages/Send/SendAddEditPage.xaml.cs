using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public partial class SendAddEditPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        private AppOptions _appOptions;
        private SendAddEditPageViewModel _vm;

        public Action AfterSubmit { get; set; }

        public SendAddEditPage(
            AppOptions appOptions = null,
            string sendId = null,
            SendType? type = null)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as SendAddEditPageViewModel;
            _vm.Page = this;
            _vm.SendId = sendId;
            _vm.Type = appOptions?.CreateSend?.Item1 ?? type;
            SetActivityIndicator();
            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                if (_vm.EditMode)
                {
                    ToolbarItems.Add(_removePassword);
                    ToolbarItems.Add(_copyLink);
                    ToolbarItems.Add(_shareLink);
                    ToolbarItems.Add(_deleteItem);
                }
                _vm.SegmentedButtonHeight = 36;
                _vm.SegmentedButtonFontSize = 13;
                _vm.SegmentedButtonMargins = new Thickness(0, 10, 0, 0);
                _vm.EditorMargins = new Thickness(0, 5, 0, 0);
                _btnOptions.WidthRequest = 70;
                _btnOptionsDown.WidthRequest = 30;
                _btnOptionsUp.WidthRequest = 30;
            }
            else if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                ToolbarItems.Add(_closeItem);
                if (_vm.EditMode)
                {
                    ToolbarItems.Add(_moreItem);
                }
                _vm.SegmentedButtonHeight = 30;
                _vm.SegmentedButtonFontSize = 15;
                _vm.SegmentedButtonMargins = new Thickness(0, 5, 0, 0);
                _vm.ShowEditorSeparators = true;
                _vm.EditorMargins = new Thickness(0, 10, 0, 5);
                _deletionDateTypePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _expirationDateTypePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }

            _deletionDateTypePicker.ItemDisplayBinding = new Binding("Key");
            _expirationDateTypePicker.ItemDisplayBinding = new Binding("Key");

            if (_vm.IsText)
            {
                _nameEntry.ReturnType = ReturnType.Next;
                _nameEntry.ReturnCommand = new Command(() => _textEditor.Focus());
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();

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
                _broadcasterService.Subscribe(nameof(SendAddEditPage), message =>
                {
                    if (message.Command == "selectFileResult")
                    {
                        MainThread.BeginInvokeOnMainThread(() =>
                        {
                            var data = message.Data as Tuple<byte[], string>;
                            _vm.FileData = data.Item1;
                            _vm.FileName = data.Item2;
                        });
                    }
                });

                await LoadOnAppearedAsync(_scrollView, true, async () =>
                {
                    var success = await _vm.LoadAsync();
                    if (!success)
                    {
                        await CloseAsync();
                        return;
                    }
                    await HandleCreateRequest();
                    if (!_vm.EditMode && string.IsNullOrWhiteSpace(_vm.Send?.Name))
                    {
                        RequestFocus(_nameEntry);
                    }
                    AdjustToolbar();
                });
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
                await CloseAsync();
            }
        }

        private async Task CloseAsync()
        {
            await Navigation.PopModalAsync();
        }

        protected override bool OnBackButtonPressed()
        {
            if (_vm.IsAddFromShare && DeviceInfo.Platform == DevicePlatform.Android)
            {
                _appOptions.CreateSend = null;
            }
            return base.OnBackButtonPressed();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if (DeviceInfo.Platform == DevicePlatform.iOS)
            {
                _broadcasterService.Unsubscribe(nameof(SendAddEditPage));
            }
        }

        private void OnUnloaded(object sender, EventArgs e)
        {
            _deletionExtendedDatePicker?.DisconnectHandler();
            _expirationExtendedDatePicker?.DisconnectHandler();
        }

        private async void TextType_Clicked(object sender, EventArgs eventArgs)
        {
            await _vm.TypeChangedAsync(SendType.Text);
            _nameEntry.ReturnType = ReturnType.Next;
            _nameEntry.ReturnCommand = new Command(() => _textEditor.Focus());
            if (string.IsNullOrWhiteSpace(_vm.Send.Name))
            {
                RequestFocus(_nameEntry);
            }
        }

        private async void FileType_Clicked(object sender, EventArgs eventArgs)
        {
            await _vm.TypeChangedAsync(SendType.File);
            _nameEntry.ReturnType = ReturnType.Done;
            _nameEntry.ReturnCommand = null;
            if (string.IsNullOrWhiteSpace(_vm.Send.Name))
            {
                RequestFocus(_nameEntry);
            }
        }

        private void OnMaxAccessCountTextChanged(object sender, TextChangedEventArgs e)
        {
            var maxAccessEntry = (Microsoft.Maui.Controls.Entry)sender;

#if IOS
            // HACK: To avoid a bug that incorrectly sets the TextColor when changing text
            // programatically we need to set it to null and back to the correct color
            // MAUI issue https://github.com/dotnet/maui/pull/20100
            maxAccessEntry.TextColor = null;
            maxAccessEntry.TextColor = ThemeManager.GetResourceColor("TextColor");
#endif

            if (string.IsNullOrWhiteSpace(e.NewTextValue))
            {
                _vm.MaxAccessCount = null;
                _maxAccessCountStepper.Value = 0;
                return;
            }
            // accept only digits
            if (!int.TryParse(e.NewTextValue, out int _))
            {
                maxAccessEntry.Text = e.OldTextValue;
            }
        }

        private async void ChooseFile_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ChooseFileAsync();
            }
        }

        private void ClearExpirationDate_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.ClearExpirationDate();
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

        private async void RemovePassword_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.RemovePasswordAsync();
            }
        }

        private async void CopyLink_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.CopyLinkAsync();
            }
        }

        private async void ShareLink_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ShareLinkAsync();
            }
        }

        private async void Delete_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                if (await _vm.DeleteAsync())
                {
                    await CloseAsync();
                }
            }
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            var options = new List<string>();
            if (_vm.SendEnabled && _vm.EditMode)
            {
                if (_vm.Send.HasPassword)
                {
                    options.Add(AppResources.RemovePassword);
                }
                options.Add(AppResources.CopyLink);
                options.Add(AppResources.ShareLink);
            }

            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                _vm.EditMode ? AppResources.Delete : null, options.ToArray());
            if (selection == AppResources.RemovePassword)
            {
                await _vm.RemovePasswordAsync();
            }
            else if (selection == AppResources.CopyLink)
            {
                await _vm.CopyLinkAsync();
            }
            else if (selection == AppResources.ShareLink)
            {
                await _vm.ShareLinkAsync();
            }
            else if (selection == AppResources.Delete)
            {
                if (await _vm.DeleteAsync())
                {
                    await CloseAsync();
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
            if (!_vm.SendEnabled && _vm.EditMode && DeviceInfo.Platform == DevicePlatform.Android)
            {
                ToolbarItems.Remove(_removePassword);
                ToolbarItems.Remove(_copyLink);
                ToolbarItems.Remove(_shareLink);
            }
        }

        private async Task HandleCreateRequest()
        {
            if (_appOptions?.CreateSend == null)
            {
                return;
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
                FileType_Clicked(null, null);
            }
            else
            {
                var text = _appOptions.CreateSend.Item4;
                _vm.Send.Text.Text = text;
                TextType_Clicked(null, null);
            }
            _appOptions.CreateSend = null;
        }
    }
}
