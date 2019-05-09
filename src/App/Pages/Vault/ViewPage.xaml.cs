using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ViewPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private ViewPageViewModel _vm;

        public ViewPage(string cipherId)
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as ViewPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator(_mainContent);

            if(Device.RuntimePlatform == Device.iOS)
            {
                _absLayout.Children.Remove(_fab);
            }
            else
            {
                ToolbarItems.RemoveAt(0);
                _fab.Clicked = EditButton_Clicked;
                _mainLayout.Padding = new Thickness(0, 0, 0, 75);
            }
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
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync(), _mainContent);
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

        private async void EditToolbarItem_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PushModalAsync(new NavigationPage(new AddEditPage(_vm.CipherId)));
            }
        }

        private void EditButton_Clicked(object sender, System.EventArgs e)
        {
            EditToolbarItem_Clicked(sender, e);
        }
    }
}
