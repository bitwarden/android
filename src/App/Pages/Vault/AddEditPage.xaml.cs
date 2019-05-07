using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AddEditPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private AddEditPageViewModel _vm;

        public AddEditPage(string cipherId)
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as AddEditPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(ViewPage), async (message) =>
            {
                if(message.Command == "syncCompleted")
                {
                    var data = message.Data as Dictionary<string, object>;
                    if(data.ContainsKey("successfully"))
                    {
                        var success = data["successfully"] as bool?;
                        if(success.HasValue && success.Value)
                        {
                            await _vm.LoadAsync();
                        }
                    }
                }
            });
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync());
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(ViewPage));
            _vm.CleanUp();
        }

        private async void PasswordHistory_Tapped(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PushModalAsync(new NavigationPage(new PasswordHistoryPage(_vm.CipherId)));
            }
        }
    }
}
