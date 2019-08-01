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
        ISteamGuardService steamGuardService = new SteamGuardService();
        Action<string, string> callback;


        public SteamTOTPPage(Action<string, string> callback)
        {
            InitializeComponent();
            this.callback = callback;
        }

        private void Button_Clicked(object sender, EventArgs e)
        {
            steamGuardService.Init(_usernameEntry.Text, _passwordEntry.Text);
            if (((SteamGuardService)steamGuardService).requiresCaptcha)
            {
                _captchaWebview.Source = new Uri("https://steamcommunity.com/public/captcha.php?gid=" + ((SteamGuardService)steamGuardService).CID);
            }
        }

        private void Button_Clicked_1(object sender, EventArgs e)
        {
            if(steamGuardService.CheckEmailCode(_emailcodeEntry.Text))
            {
                steamGuardService.RequestSMSCode();
            }
        }

        private async void Button_Clicked_2(object sender, EventArgs e)
        {
            if (steamGuardService.CheckSMSCode(_smscodeEntry.Text))
            {
                System.Console.WriteLine("SUCCESS!");
                System.Console.WriteLine(steamGuardService.TOTPSecret);
                System.Console.WriteLine(steamGuardService.RecoveryCode);
                callback(steamGuardService.TOTPSecret, steamGuardService.RecoveryCode);
            }
        }

        private void Button_Clicked_3(object sender, EventArgs e)
        {
            ((SteamGuardService)steamGuardService).SetCaptcha(_captchaEntry.Text);
        }
    }
}
