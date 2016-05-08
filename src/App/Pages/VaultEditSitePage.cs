using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultEditSitePage : ContentPage
    {
        private readonly string _siteId;
        private readonly ISiteService _siteService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;

        public VaultEditSitePage(string siteId)
        {
            _siteId = siteId;
            _siteService = Resolver.Resolve<ISiteService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();

            Init();
        }

        private void Init()
        {
            var site = _siteService.GetByIdAsync(_siteId).GetAwaiter().GetResult();
            if(site == null)
            {
                // TODO: handle error. navigate back? should never happen...
                return;
            }

            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());

            var uriEntry = new Entry { Keyboard = Keyboard.Url, Text = site.Uri?.Decrypt() };
            var nameEntry = new Entry { Text = site.Name?.Decrypt() };
            var folderPicker = new Picker { Title = AppResources.Folder };
            folderPicker.Items.Add(AppResources.FolderNone);
            int selectedIndex = 0;
            int i = 0;
            foreach(var folder in folders)
            {
                i++;
                if(folder.Id == site.FolderId)
                {
                    selectedIndex = i;
                }

                folderPicker.Items.Add(folder.Name.Decrypt());
            }
            folderPicker.SelectedIndex = selectedIndex;
            var usernameEntry = new Entry { Text = site.Username?.Decrypt() };
            var passwordEntry = new Entry { IsPassword = true, Text = site.Password?.Decrypt() };
            var notesEditor = new Editor { Text = site.Notes?.Decrypt() };

            var stackLayout = new StackLayout();
            stackLayout.Children.Add(new Label { Text = AppResources.URI });
            stackLayout.Children.Add(uriEntry);
            stackLayout.Children.Add(new Label { Text = AppResources.Name });
            stackLayout.Children.Add(nameEntry);
            stackLayout.Children.Add(new Label { Text = AppResources.Folder });
            stackLayout.Children.Add(folderPicker);
            stackLayout.Children.Add(new Label { Text = AppResources.Username });
            stackLayout.Children.Add(usernameEntry);
            stackLayout.Children.Add(new Label { Text = AppResources.Password });
            stackLayout.Children.Add(passwordEntry);
            stackLayout.Children.Add(new Label { Text = AppResources.Notes });
            stackLayout.Children.Add(notesEditor);

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            var saveToolBarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(string.IsNullOrWhiteSpace(uriEntry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.URI), AppResources.Ok);
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameEntry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Name), AppResources.Ok);
                    return;
                }

                site.Uri = uriEntry.Text.Encrypt();
                site.Name = nameEntry.Text.Encrypt();
                site.Username = usernameEntry.Text?.Encrypt();
                site.Password = passwordEntry.Text?.Encrypt();
                site.Notes = notesEditor.Text?.Encrypt();

                if(folderPicker.SelectedIndex > 0)
                {
                    site.FolderId = folders.ElementAt(folderPicker.SelectedIndex - 1).Id;
                }

                var saveTask = _siteService.SaveAsync(site);
                _userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                _userDialogs.HideLoading();
                await Navigation.PopAsync();
                _userDialogs.SuccessToast(nameEntry.Text, "Site updated.");
            }, ToolbarItemOrder.Default, 0);

            Title = "Edit Site";
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);

            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
        }
    }
}
