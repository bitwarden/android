using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Services.Steam;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages.Vault
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class SteamTOTPPage : ContentPage
    {
        public SteamTOTPPage(Action<string, string> callback)
        {
            InitializeComponent();

            (BindingContext as SteamTOTPPageViewModel).SteamLinkedCallback = callback;
        }
    }
}
