using System;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class SteamTotpPage : ContentPage
    {
        private SteamTOTPPageViewModel _vm;

        public SteamTotpPage(Action<string, string> callback, string password = "", string username = "")
        {
            InitializeComponent();

            _vm = (BindingContext as SteamTOTPPageViewModel);
            _vm.SteamLinkedCallback = callback;
            _vm.Password = password;
            _vm.Username = username;
        }
    }
}
