using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class ToolsPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;

        public ToolsPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();

            Init();
        }

        public void Init()
        {
            Title = AppResources.Tools;
            Icon = "fa-refresh";
        }
    }
}
