using System;
using System.Collections.Generic;
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
            var uriCell = new FormEntryCell(AppResources.URI, Keyboard.Url);
            var nameCell = new FormEntryCell(AppResources.Name);
            var usernameCell = new FormEntryCell(AppResources.Username);
            var passwordCell = new FormEntryCell(AppResources.Password, IsPassword: true);

            var folderOptions = new List<string> { AppResources.FolderNone };
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());
            foreach(var folder in folders)
            {
                folderOptions.Add(folder.Name.Decrypt());
            }
            var folderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());

            var notesCell = new FormEditorCell(height:90);

            var mainTable = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection("Site Information")
                    {
                        uriCell,
                        nameCell,
                        folderCell,
                        usernameCell,
                        passwordCell
                    },
                    new TableSection(AppResources.Notes)
                    {
                        notesCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                mainTable.RowHeight = -1;
                mainTable.EstimatedRowHeight = 70;
            }

            var scrollView = new ScrollView
            {
                Content = mainTable,
                Orientation = ScrollOrientation.Vertical
            };

            var saveToolBarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(string.IsNullOrWhiteSpace(uriCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.URI), AppResources.Ok);
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Name), AppResources.Ok);
                    return;
                }

                var site = new Site
                {
                    Uri = uriCell.Entry.Text.Encrypt(),
                    Name = nameCell.Entry.Text.Encrypt(),
                    Username = usernameCell.Entry.Text?.Encrypt(),
                    Password = passwordCell.Entry.Text?.Encrypt(),
                    Notes = notesCell.Editor.Text?.Encrypt(),
                };

                if(folderCell.Picker.SelectedIndex > 0)
                {
                    site.FolderId = folders.ElementAt(folderCell.Picker.SelectedIndex - 1).Id;
                }

                var saveTask = _siteService.SaveAsync(site);
                _userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                _userDialogs.HideLoading();
                await Navigation.PopAsync();
                _userDialogs.SuccessToast(nameCell.Entry.Text, "New site created.");
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.AddSite;
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);
            ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel"));

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

