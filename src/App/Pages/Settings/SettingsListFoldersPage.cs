using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Bit.App.Utilities;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class SettingsListFoldersPage : ExtendedContentPage
    {
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        public SettingsListFoldersPage()
        {
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();

            Init();
        }

        public ExtendedObservableCollection<SettingsFolderPageModel> Folders { get; private set; }
            = new ExtendedObservableCollection<SettingsFolderPageModel>();
        public ListView ListView { get; set; }
        private AddFolderToolBarItem AddItem { get; set; }

        private void Init()
        {
            AddItem = new AddFolderToolBarItem(this);
            ToolbarItems.Add(AddItem);

            ListView = new ListView
            {
                ItemsSource = Folders,
                ItemTemplate = new DataTemplate(() => new SettingsFolderListViewCell(this))
            };
             
            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Windows)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.Folders;
            Content = ListView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            ListView.ItemSelected += FolderSelected;
            AddItem.InitEvents();
            LoadFoldersAsync().Wait();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            ListView.ItemSelected -= FolderSelected;
            AddItem.Dispose();
        }

        private async Task LoadFoldersAsync()
        {
            var folders = await _folderService.GetAllAsync();
            var pageFolders = folders.Select(f => new SettingsFolderPageModel(f)).OrderBy(f => f.Name);
            Folders.ResetWithRange(pageFolders);
        }

        private async void FolderSelected(object sender, SelectedItemChangedEventArgs e)
        {
            var folder = e.SelectedItem as SettingsFolderPageModel;
            var page = new SettingsEditFolderPage(folder.Id);
            await Navigation.PushForDeviceAsync(page);
        }

        private class AddFolderToolBarItem : ExtendedToolbarItem
        {
            private readonly SettingsListFoldersPage _page;

            public AddFolderToolBarItem(SettingsListFoldersPage page)
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "plus.png";
                ClickAction = () => ClickedItem();
            }

            private async void ClickedItem()
            {
                var page = new SettingsAddFolderPage();
                await _page.Navigation.PushForDeviceAsync(page);
            }
        }

        private class SettingsFolderListViewCell : ExtendedTextCell
        {
            public SettingsFolderListViewCell(SettingsListFoldersPage page)
            {
                this.SetBinding(TextProperty, nameof(SettingsFolderPageModel.Name));
            }
        }
    }
}
