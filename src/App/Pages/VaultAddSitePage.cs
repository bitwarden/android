using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
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
        public VaultAddSitePage()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            var siteService = Resolver.Resolve<ISiteService>();
            var folderService = Resolver.Resolve<IFolderService>();
            var userDialogs = Resolver.Resolve<IUserDialogs>();
            var connectivity = Resolver.Resolve<IConnectivity>();

            var folders = folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());

            var uriEntry = new Entry { Keyboard = Keyboard.Url };
            var nameEntry = new Entry();
            var folderPicker = new Picker { Title = "Folder" };
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
                if(!connectivity.IsConnected)
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

                var saveTask = siteService.SaveAsync(site);
                userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                userDialogs.HideLoading();
                await Navigation.PopAsync();
                userDialogs.SuccessToast(nameEntry.Text, "New site created.");
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.AddSite;
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);

            if(!connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        public void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
        }
    }
}

