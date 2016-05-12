using System;
using System.Linq;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
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

            Init();
        }

        private void Init()
        {
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());

            var uriEntry = new ExtendedEntry { Keyboard = Keyboard.Url, HasBorder = false };
            var nameEntry = new ExtendedEntry { HasBorder = false };
            var folderPicker = new ExtendedPicker { Title = AppResources.Folder, HasBorder = false };
            folderPicker.Items.Add(AppResources.FolderNone);
            folderPicker.SelectedIndex = 0;
            foreach(var folder in folders)
            {
                folderPicker.Items.Add(folder.Name.Decrypt());
            }
            var usernameEntry = new ExtendedEntry { HasBorder = false };
            var passwordEntry = new ExtendedEntry { IsPassword = true, HasBorder = false };
            var notesEditor = new ExtendedEditor { HeightRequest = Device.OS == TargetPlatform.iOS ? 70 : 90, HasBorder = false };

            var uriStackLayout = new FormEntryStackLayout();
            uriStackLayout.Children.Add(new EntryLabel { Text = AppResources.URI });
            uriStackLayout.Children.Add(uriEntry);
            var uriCell = new ViewCell();
            uriCell.View = uriStackLayout;

            var nameStackLayout = new FormEntryStackLayout();
            nameStackLayout.Children.Add(new EntryLabel { Text = AppResources.Name });
            nameStackLayout.Children.Add(nameEntry);
            var nameCell = new ViewCell();
            nameCell.View = nameStackLayout;

            var folderStackLayout = new FormEntryStackLayout();
            folderStackLayout.Children.Add(new EntryLabel { Text = AppResources.Folder });
            folderStackLayout.Children.Add(folderPicker);
            var folderCell = new ViewCell();
            folderCell.View = folderStackLayout;

            var usernameStackLayout = new FormEntryStackLayout();
            usernameStackLayout.Children.Add(new EntryLabel { Text = AppResources.Username });
            usernameStackLayout.Children.Add(usernameEntry);
            var usernameCell = new ViewCell();
            usernameCell.View = usernameStackLayout;

            var passwordStackLayout = new FormEntryStackLayout();
            passwordStackLayout.Children.Add(new EntryLabel { Text = AppResources.Password });
            passwordStackLayout.Children.Add(passwordEntry);
            var passwordCell = new ViewCell();
            passwordCell.View = passwordStackLayout;

            var notesStackLayout = new FormEntryStackLayout();
            notesStackLayout.Children.Add(notesEditor);
            var notesCell = new ViewCell();
            notesCell.View = notesStackLayout;

            var mainTable = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        uriCell,
                        nameCell,
                        folderCell,
                        usernameCell,
                        passwordCell
                    }
                }
            };

            var notesTable = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection(AppResources.Notes)
                    {
                        notesCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                mainTable.RowHeight = 70;
                notesTable.RowHeight = 90;
            }

            var tablesStackLayout = new StackLayout();
            tablesStackLayout.Children.Add(mainTable);
            tablesStackLayout.Children.Add(notesTable);

            var scrollView = new ScrollView
            {
                Content = tablesStackLayout,
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

        private class FormEntryStackLayout : StackLayout
        {
            public FormEntryStackLayout()
            {
                Padding = new Thickness(15, 15, 15, 0);
                BackgroundColor = Color.White;
            }
        }
    }
}

