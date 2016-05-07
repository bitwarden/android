using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Models.View;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultListPage : ContentPage
    {
        private readonly IFolderService _folderService;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;

        public VaultListPage()
        {
            _folderService = Resolver.Resolve<IFolderService>();
            _siteService = Resolver.Resolve<ISiteService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }

        public ObservableCollection<VaultView.Folder> Folders { get; private set; } = new ObservableCollection<VaultView.Folder>();

        private void Init()
        {
            ToolbarItems.Add(new AddSiteToolBarItem(this));

            var listView = new ListView { IsGroupingEnabled = true, ItemsSource = Folders };
            listView.GroupDisplayBinding = new Binding("Name");
            listView.ItemSelected += SiteSelected;
            listView.ItemTemplate = new DataTemplate(() => new VaultListViewCell(this));

            Title = AppResources.MyVault;
            Content = listView;
            NavigationPage.SetBackButtonTitle(this, string.Empty);
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
            var site = e.SelectedItem as VaultView.Site;
            Navigation.PushAsync(new VaultViewSitePage(site.Id));
        }

        private async void MoreClickedAsync(object sender, EventArgs e)
        {
            var mi = sender as MenuItem;
            var site = mi.CommandParameter as VaultView.Site;
            var selection = await DisplayActionSheet(AppResources.MoreOptions, AppResources.Cancel, null,
                AppResources.View, AppResources.Edit, AppResources.CopyPassword, AppResources.CopyUsername, AppResources.GoToWebsite);

            if(selection == AppResources.View)
            {
                await Navigation.PushAsync(new VaultViewSitePage(site.Id));
            }
            else if(selection == AppResources.Edit)
            {
                // TODO: navigate to edit page
            }
            else if(selection == AppResources.CopyPassword)
            {
                Copy(site.Password, AppResources.Password);
            }
            else if(selection == AppResources.CopyUsername)
            {
                Copy(site.Username, AppResources.Username);
            }
            else if(selection == AppResources.GoToWebsite)
            {
                Device.OpenUri(new Uri(site.Uri));
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.SuccessToast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private async void DeleteClickedAsync(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.DoYouReallyWantToDelete, null, AppResources.Yes, AppResources.No))
            {
                return;
            }

            var mi = sender as MenuItem;
            var site = mi.CommandParameter as VaultView.Site;
            var deleteCall = await _siteService.DeleteAsync(site.Id);

            if(deleteCall.Succeeded)
            {
                var folder = Folders.Single(f => f.Id == site.FolderId);
                var siteIndex = folder.Select((s, i) => new { s, i }).First(s => s.s.Id == site.Id).i;
                folder.RemoveAt(siteIndex);
                _userDialogs.SuccessToast(AppResources.SiteDeleted);
            }
            else if(deleteCall.Errors.Count() > 0)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, deleteCall.Errors.First().Message, AppResources.Ok);
            }
        }

        private class AddSiteToolBarItem : ToolbarItem
        {
            private readonly VaultListPage _page;

            public AddSiteToolBarItem(VaultListPage page)
            {
                _page = page;
                Text = AppResources.Add;
                Icon = "fa-plus";
                Clicked += ClickedItem;
            }

            private async void ClickedItem(object sender, EventArgs e)
            {
                await _page.Navigation.PushAsync(new VaultAddSitePage());
            }
        }

        private class VaultListViewCell : TextCell
        {
            public VaultListViewCell(VaultListPage page)
            {
                var moreAction = new MenuItem { Text = AppResources.More };
                moreAction.SetBinding(MenuItem.CommandParameterProperty, new Binding("."));
                moreAction.Clicked += page.MoreClickedAsync;

                var deleteAction = new MenuItem { Text = AppResources.Delete, IsDestructive = true };
                deleteAction.SetBinding(MenuItem.CommandParameterProperty, new Binding("."));
                deleteAction.Clicked += page.DeleteClickedAsync;

                this.SetBinding<VaultView.Site>(TextProperty, s => s.Name);
                this.SetBinding<VaultView.Site>(DetailProperty, s => s.Username);
                ContextActions.Add(moreAction);
                ContextActions.Add(deleteAction);
            }
        }
    }
}
