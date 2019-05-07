using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class GroupingsPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly GroupingsPageViewModel _vm;

        public GroupingsPage()
            : this(true)
        { }

        public GroupingsPage(bool mainPage, CipherType? type = null, string folderId = null,
            string collectionId = null, string pageTitle = null)
        {
            InitializeComponent();
            SetActivityIndicator();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vm = BindingContext as GroupingsPageViewModel;
            _vm.Page = this;
            _vm.MainPage = mainPage;
            _vm.Type = type;
            _vm.FolderId = folderId;
            _vm.CollectionId = collectionId;
            if(pageTitle != null)
            {
                _vm.PageTitle = pageTitle;
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            // await _syncService.FullSyncAsync(true);
            _broadcasterService.Subscribe(nameof(GroupingsPage), async (message) =>
            {
                if(message.Command == "syncCompleted")
                {
                    await Task.Delay(500);
                    // await _viewModel.LoadAsync();
                }
            });

            await LoadOnAppearedAsync(_mainLayout, false, async () =>
            {
                if(!_syncService.SyncInProgress)
                {
                    await _vm.LoadAsync();
                }
                else
                {
                    await Task.Delay(5000);
                    if(!_vm.Loaded)
                    {
                        await _vm.LoadAsync();
                    }
                }
            });
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(GroupingsPage));
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if(!DoOnce())
            {
                return;
            }

            if(!(e.SelectedItem is GroupingsPageListItem item))
            {
                return;
            }

            if(item.Cipher != null)
            {
                await _vm.SelectCipherAsync(item.Cipher);
            }
            else if(item.Folder != null)
            {
                await _vm.SelectFolderAsync(item.Folder);
            }
            else if(item.Collection != null)
            {
                await _vm.SelectCollectionAsync(item.Collection);
            }
            else if(item.Type != null)
            {
                await _vm.SelectTypeAsync(item.Type.Value);
            }
        }

        private async void Search_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new CiphersPage(_vm.Filter, _vm.FolderId != null, _vm.CollectionId != null,
                    _vm.Type != null);
                await Navigation.PushModalAsync(new NavigationPage(page), false);
            }
        }
    }
}
