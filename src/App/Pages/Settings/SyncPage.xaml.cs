using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SyncPage : BaseContentPage
    {
        private readonly SyncPageViewModel _vm;

        public SyncPage()
        {
            InitializeComponent();
            _vm = BindingContext as SyncPageViewModel;
            _vm.Page = this;
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.SetLastSyncAsync();
        }

        private async void Sync_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SyncAsync();
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
