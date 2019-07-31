using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages.Vault
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class SteamTOTPPage : ContentPage
    {
        public SteamTOTPPage()
        {
            InitializeComponent();
        }
    }
}