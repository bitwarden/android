using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LockPage : BaseContentPage
    {
        private LockPageViewModel _vm;

        public LockPage()
        {
            InitializeComponent();
            _vm = BindingContext as LockPageViewModel;
            _vm.Page = this;
            MasterPasswordEntry = _masterPassword;
            PinEntry = _pin;
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry PinEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if(_vm.PinLock)
            {
                RequestFocus(PinEntry);
            }
            else
            {
                RequestFocus(MasterPasswordEntry);
            }
        }

        private async void Unlock_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void LogOut_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.LogOutAsync();
            }
        }
    }
}
