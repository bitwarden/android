using System;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AttachmentsPage : BaseContentPage
    {
        private AttachmentsPageViewModel _vm;
        private readonly IBroadcasterService _broadcasterService;

        public AttachmentsPage(string cipherId)
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as AttachmentsPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(AttachmentsPage), (message) =>
            {
                if (message.Command == "selectFileResult")
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        var data = message.Data as Tuple<byte[], string>;
                        _vm.FileData = data.Item1;
                        _vm.FileName = data.Item2;
                    });
                }
            });
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.InitAsync());
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if (Device.RuntimePlatform != Device.iOS)
            {
                _broadcasterService.Unsubscribe(nameof(AttachmentsPage));
            }
        }

        private async void Save_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void ChooseFile_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ChooseFileAsync();
            }
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
