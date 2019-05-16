using System;
using System.Threading.Tasks;
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
            else if(!_vm.FingerprintLock)
            {
                RequestFocus(MasterPasswordEntry);
            }
        }

        private void Unlock_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(50);
                    Device.BeginInvokeOnMainThread(async () =>
                    {
                        await _vm.SubmitAsync();
                    });
                });
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
