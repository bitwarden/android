using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ViewPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private ViewPageViewModel _vm;

        public ViewPage(string cipherId)
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vm = BindingContext as ViewPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator(_mainContent);

            if(Device.RuntimePlatform == Device.iOS)
            {
                _absLayout.Children.Remove(_fab);
                ToolbarItems.Add(_closeItem);
                ToolbarItems.Add(_editItem);
                ToolbarItems.Add(_moreItem);
            }
            else
            {
                _fab.Clicked = EditButton_Clicked;
                _mainLayout.Padding = new Thickness(0, 0, 0, 75);
                ToolbarItems.Add(_attachmentsItem);
                ToolbarItems.Add(_deleteItem);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            if(_syncService.SyncInProgress)
            {
                IsBusy = true;
            }

            _broadcasterService.Subscribe(nameof(ViewPage), async (message) =>
            {
                if(message.Command == "syncStarted")
                {
                    Device.BeginInvokeOnMainThread(() => IsBusy = true);
                }
                else if(message.Command == "syncCompleted")
                {
                    await Task.Delay(500);
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        IsBusy = false;
                        if(message.Data is Dictionary<string, object> data && data.ContainsKey("successfully"))
                        {
                            var success = data["successfully"] as bool?;
                            if(success.GetValueOrDefault())
                            {
                                var task = _vm.LoadAsync(() => AdjustToolbar());
                            }
                        }
                    });
                }
            });
            await LoadOnAppearedAsync(_scrollView, true, async () =>
            {
                var success = await _vm.LoadAsync(() => AdjustToolbar());
                if(!success)
                {
                    await Navigation.PopModalAsync();
                }
            }, _mainContent);
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            IsBusy = false;
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

        private async void Attachments_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new AttachmentsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Share_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new SharePage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Delete_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                if(await _vm.DeleteAsync())
                {
                    await Navigation.PopModalAsync();
                }
            }
        }

        private async void Collections_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new CollectionsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            if(!DoOnce())
            {
                return;
            }
            var options = new List<string> { AppResources.Attachments };
            options.Add(_vm.Cipher.OrganizationId == null ? AppResources.Share : AppResources.Collections);
            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                AppResources.Delete, options.ToArray());
            if(selection == AppResources.Delete)
            {
                if(await _vm.DeleteAsync())
                {
                    await Navigation.PopModalAsync();
                }
            }
            else if(selection == AppResources.Attachments)
            {
                var page = new AttachmentsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
            else if(selection == AppResources.Collections)
            {
                var page = new CollectionsPage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
            else if(selection == AppResources.Share)
            {
                var page = new SharePage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private void AdjustToolbar()
        {
            if(Device.RuntimePlatform != Device.Android || _vm.Cipher == null)
            {
                return;
            }
            if(_vm.Cipher.OrganizationId == null)
            {
                if(ToolbarItems.Contains(_collectionsItem))
                {
                    ToolbarItems.Remove(_collectionsItem);
                }
                if(!ToolbarItems.Contains(_shareItem))
                {
                    ToolbarItems.Insert(1, _shareItem);
                }
            }
            else
            {
                if(ToolbarItems.Contains(_shareItem))
                {
                    ToolbarItems.Remove(_shareItem);
                }
                if(!ToolbarItems.Contains(_collectionsItem))
                {
                    ToolbarItems.Insert(1, _collectionsItem);
                }
            }
        }
    }
}
