using System;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class ExportVaultPage : BaseContentPage
    {
        private readonly ExportVaultPageViewModel _vm;
        private readonly IBroadcasterService _broadcasterService;

        public ExportVaultPage()
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as ExportVaultPageViewModel;
            _vm.Page = this;
            _fileFormatPicker.ItemDisplayBinding = new Binding("Value");
            SecretEntry = _secret;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            _broadcasterService.Subscribe(nameof(ExportVaultPage), (message) =>
            {
                if (message.Command == "selectSaveFileResult")
                {
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        var data = message.Data as Tuple<string, string>;
                        if (data == null)
                        {
                            return;
                        }
                        _vm.SaveFileSelected(data.Item1, data.Item2);
                    });
                }
            });
            RequestFocus(_secret);
        }

        protected async override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(ExportVaultPage));
        }

        public Entry SecretEntry { get; set; }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void ExportVault_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ExportVaultAsync();
            }
        }

        private async void RequestOTP_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.RequestOTP();
                _requestOTP.IsEnabled = false;
            }
        }

        void FileFormat_Changed(object sender, EventArgs e)
        {
            _vm?.UpdateWarning();
        }
    }
}
