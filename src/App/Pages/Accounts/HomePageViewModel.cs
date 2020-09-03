using System;
using Bit.App.Resources;

namespace Bit.App.Pages
{
    public class HomeViewModel : BaseViewModel
    {
        public HomeViewModel()
        {
            PageTitle = AppResources.Bitwarden;
        }
        
        public Action StartLoginAction { get; set; }
        public Action StartRegisterAction { get; set; }
        public Action StartSsoLoginAction { get; set; }
        public Action StartEnvironmentAction { get; set; }
        public Action CloseAction { get; set; }
    }
}
