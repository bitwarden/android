using Bit.App.Resources;
using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public partial class AuthenticatorPage : BaseContentPage
    {
        #region Members

        private readonly IBroadcasterService _broadcasterService;
        private AuthenticatorPageViewModel _vm;
        private readonly bool _fromTabPage;
        private readonly Action<string> _selectAction;
        private readonly TabsPage _tabsPage;

        #endregion

        public AuthenticatorPage(bool fromTabPage, Action<string> selectAction = null, TabsPage tabsPage = null)
        {
            //_tabsPage = tabsPage;
            InitializeComponent();
            //_broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as AuthenticatorPageViewModel;
            //_vm.Page = this;
            //_fromTabPage = fromTabPage;
            //_selectAction = selectAction;

            if (Device.RuntimePlatform == Device.iOS)
            {
                _absLayout.Children.Remove(_fab);
                ToolbarItems.Add(_aboutIconItem);
                ToolbarItems.Add(_addItem);
            }
            else
            {
                ToolbarItems.Add(_syncItem);
                ToolbarItems.Add(_lockItem);
                ToolbarItems.Add(_aboutTextItem);
            }
        }

        public async Task InitAsync()
        {
            await _vm.InitAsync();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            //if (!_fromTabPage)
            //{
            //    await InitAsync();
            //}
            //_broadcasterService.Subscribe(nameof(GeneratorPage), async (message) =>
            //{
            //    if (message.Command == "updatedTheme")
            //    {
            //        Device.BeginInvokeOnMainThread(() =>
            //        {
            //            //_vm.RedrawPassword();
            //        });
            //    }
            //});
        }
        

        private async void Search_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                // var page = new SendsPage(_vm.Filter, _vm.Type != null);
                // await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Sync_Clicked(object sender, EventArgs e)
        {
            // await _vm.SyncAsync();
        }

        private async void Lock_Clicked(object sender, EventArgs e)
        {
            // await _vaultTimeoutService.LockAsync(true, true);
        }
        
        private void About_Clicked(object sender, EventArgs e)
        {
            // _vm.ShowAbout();
        }

        private async void AddButton_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                // var page = new SendAddEditPage(null, null, _vm.Type);
                // await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
        }

        private async void Copy_Clicked(object sender, EventArgs e)
        {
            //await _vm.CopyAsync();
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            //if (!DoOnce())
            //{
            //    return;
            //}
            //var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
            //    null, AppResources.PasswordHistory);
            //if (selection == AppResources.PasswordHistory)
            //{
            //    var page = new GeneratorHistoryPage();
            //    await Navigation.PushModalAsync(new Xamarin.Forms.NavigationPage(page));
            //}
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            //_broadcasterService.Unsubscribe(nameof(GeneratorPage));
        }
        
    }
}
