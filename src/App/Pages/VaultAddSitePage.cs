using System;
using System.Linq;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultAddSitePage : ContentPage
    {
        private readonly ISiteService _siteService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;

        public VaultAddSitePage()
        {
            _siteService = Resolver.Resolve<ISiteService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();
        }

        private void Init()
        {
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());

            var uriEntry = new Entry { Keyboard = Keyboard.Url };
            var nameEntry = new Entry();
            var folderPicker = new Picker { Title = AppResources.Folder };
            folderPicker.Items.Add(AppResources.FolderNone);
            folderPicker.SelectedIndex = 0;
            foreach(var folder in folders)
            {
                folderPicker.Items.Add(folder.Name.Decrypt());
            }
            var usernameEntry = new Entry();
            var passwordEntry = new Entry { IsPassword = true };
            var notesEditor = new Editor();

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

                var site = new Site
                {
                    Uri = uriEntry.Text.Encrypt(),
                    Name = nameEntry.Text.Encrypt(),
                    Username = usernameEntry.Text?.Encrypt(),
                    Password = passwordEntry.Text?.Encrypt(),
                    Notes = notesEditor.Text?.Encrypt(),
                };

                if(folderPicker.SelectedIndex > 0)
                {
                    site.FolderId = folders.ElementAt(folderPicker.SelectedIndex - 1).Id;
                }

                var saveTask = _siteService.SaveAsync(site);
                _userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                _userDialogs.HideLoading();
                await Navigation.PopAsync();
                _userDialogs.SuccessToast(nameEntry.Text, "New site created.");
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.AddSite;
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

