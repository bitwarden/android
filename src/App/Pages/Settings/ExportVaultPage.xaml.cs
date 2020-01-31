using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ExportVaultPage : BaseContentPage
    {
        private readonly ExportVaultPageViewModel _vm;

        public ExportVaultPage()
        {
            InitializeComponent();
            _vm = BindingContext as ExportVaultPageViewModel;
            _vm.Page = this;
            _fileFormatPicker.ItemDisplayBinding = new Binding("Value");
            MasterPasswordEntry = _masterPassword;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            RequestFocus(_masterPassword);
        }

        protected async override void OnDisappearing()
        {
            base.OnDisappearing();
        }

        public Entry MasterPasswordEntry { get; set; }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void ExportVault_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.ExportVaultAsync();
            }
        }
    }
}
