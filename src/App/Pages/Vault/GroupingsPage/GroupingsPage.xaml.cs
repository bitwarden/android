using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class GroupingsPage : ContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly GroupingsPageViewModel _viewModel;

        public GroupingsPage()
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _viewModel = BindingContext as GroupingsPageViewModel;
            _viewModel.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(GroupingsPage), async (message) =>
            {
                if(message.Command == "syncCompleted")
                {
                    await Task.Delay(500);
                    // await _viewModel.LoadAsync();
                }
            });

            if(!_syncService.SyncInProgress)
            {
                await _viewModel.LoadAsync();
            }
            else
            {
                await Task.Delay(5000);
                if(!_viewModel.Loaded)
                {
                    await _viewModel.LoadAsync();
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(GroupingsPage));
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            if(!(e.SelectedItem is GroupingsPageListItem item))
            {
                return;
            }

            if(item.Cipher != null)
            {
                await _viewModel.SelectCipherAsync(item.Cipher);
            }
            else if(item.Folder != null || item.Collection != null)
            {
                // TODO
            }
        }
    }
}
