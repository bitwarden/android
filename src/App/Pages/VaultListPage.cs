using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Models.View;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultListPage : ContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;

        public VaultListPage()
        {
            _folderService = Resolver.Resolve<IFolderService>();
            _siteService = Resolver.Resolve<ISiteService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();

            Init();
        }

        public ObservableCollection<VaultView.Folder> Folders { get; private set; } = new ObservableCollection<VaultView.Folder>();

        private void Init()
        {
            ToolbarItems.Add(new AddSiteToolBarItem(this));

            var moreAction = new MenuItem { Text = "More" };
            moreAction.SetBinding(MenuItem.CommandParameterProperty, new Binding("."));
            moreAction.Clicked += MoreClickedAsync;

            var deleteAction = new MenuItem { Text = "Delete", IsDestructive = true };
            deleteAction.SetBinding(MenuItem.CommandParameterProperty, new Binding("."));
            deleteAction.Clicked += DeleteClickedAsync;

            var listView = new ListView { IsGroupingEnabled = true, ItemsSource = Folders };
            listView.GroupDisplayBinding = new Binding("Name");
            listView.ItemSelected += SiteSelected;
            listView.ItemTemplate = new DataTemplate(() => new VaultListViewCell(moreAction, deleteAction));

            Title = "My Vault";
            Content = listView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            LoadFoldersAsync().Wait();
        }

        private async Task LoadFoldersAsync()
        {
            Folders.Clear();

            var folders = await _folderService.GetAllAsync();
            var sites = await _siteService.GetAllAsync();

            foreach(var folder in folders)
            {
                var f = new VaultView.Folder(folder, sites.Where(s => s.FolderId == folder.Id));
                Folders.Add(f);
            }

            // add the sites with no folder
            var noneFolder = new VaultView.Folder(sites.Where(s => s.FolderId == null));
            Folders.Add(noneFolder);
        }

        private void SiteSelected(object sender, SelectedItemChangedEventArgs e)
        {

        }

        private async void MoreClickedAsync(object sender, EventArgs e)
        {
            var mi = sender as MenuItem;
            var site = mi.CommandParameter as VaultView.Site;
            var selection = await DisplayActionSheet("More Options", "Cancel", null, "View", "Edit", "Copy Password", "Copy Username", "Go To Website");

            switch(selection)
            {
                case "View":
                case "Edit":
                case "Copy Password":
                case "Copy Username":
                case "Go To Website":
                default:
                    break;
            }
        }

        private async void DeleteClickedAsync(object sender, EventArgs e)
        {
            var mi = sender as MenuItem;
            var site = mi.CommandParameter as VaultView.Site;
            var deleteCall = await _siteService.DeleteAsync(site.Id);

            if(deleteCall.Succeeded)
            {
                var folder = Folders.Single(f => f.Id == site.FolderId);
                var siteIndex = folder.Select((s, i) => new { s, i }).First(s => s.s.Id == site.Id).i;
                folder.RemoveAt(siteIndex);
                _userDialogs.SuccessToast("Site deleted.");
            }
            else if(deleteCall.Errors.Count() > 0)
            {
                await DisplayAlert("An error has occurred", deleteCall.Errors.First().Message, "Ok");
            }
        }

        private class AddSiteToolBarItem : ToolbarItem
        {
            private readonly VaultListPage _page;

            public AddSiteToolBarItem(VaultListPage page)
            {
                _page = page;
                Text = "Add";
                Icon = "";
                Clicked += ClickedItem;
            }

            private async void ClickedItem(object sender, EventArgs e)
            {
                var selection = await _page.DisplayActionSheet("Add", "Cancel", null, "Add New Folder", "Add New Site");
                if(selection == "Add New Folder")
                {
                    var addFolderPage = new VaultAddFolderPage();
                    await _page.Navigation.PushAsync(addFolderPage);
                }
                else if(selection == "Add New Site")
                {
                    var addSitePage = new VaultAddSitePage();
                    await _page.Navigation.PushAsync(addSitePage);
                }
            }
        }

        private class VaultListViewCell : TextCell
        {
            public VaultListViewCell(MenuItem moreMenuItem, MenuItem deleteMenuItem)
            {
                this.SetBinding<VaultView.Site>(TextProperty, s => s.Name);
                this.SetBinding<VaultView.Site>(DetailProperty, s => s.Username);
                ContextActions.Add(moreMenuItem);
                ContextActions.Add(deleteMenuItem);
            }
        }
    }
}
