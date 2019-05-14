using System;

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
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.SetLastSyncAsync();
        }

        private async void Sync_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SyncAsync();
            }
        }
    }
}
