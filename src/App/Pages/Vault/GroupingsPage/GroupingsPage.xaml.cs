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
        private readonly GroupingsPageViewModel _viewModel;

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
            _viewModel = BindingContext as GroupingsPageViewModel;
            _viewModel.Page = this;
            _viewModel.MainPage = mainPage;
            _viewModel.Type = type;
            _viewModel.FolderId = folderId;
            _viewModel.CollectionId = collectionId;
            if(pageTitle != null)
            {
                _viewModel.PageTitle = pageTitle;
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
            });
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
            else if(item.Folder != null)
            {
                await _viewModel.SelectFolderAsync(item.Folder);
            }
            else if(item.Collection != null)
            {
                await _viewModel.SelectCollectionAsync(item.Collection);
            }
            else if(item.Type != null)
            {
                await _viewModel.SelectTypeAsync(item.Type.Value);
            }
        }

        private async void Search_Clicked(object sender, System.EventArgs e)
        {
            await Navigation.PushModalAsync(new CiphersPage(), false);
        }
    }
}
