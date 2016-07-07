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
    public class VaultAddSitePage : ExtendedContentPage
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

        public FormEntryCell PasswordCell { get; private set; }

        private void Init()
        {
            var notesCell = new FormEditorCell(height: 90);
            PasswordCell = new FormEntryCell(AppResources.Password, IsPassword: true, nextElement: notesCell.Editor);
            var usernameCell = new FormEntryCell(AppResources.Username, nextElement: PasswordCell.Entry);
            usernameCell.Entry.DisableAutocapitalize = true;
            usernameCell.Entry.Autocorrect = false;

            usernameCell.Entry.FontFamily = PasswordCell.Entry.FontFamily = "Courier";

            var uriCell = new FormEntryCell(AppResources.URI, Keyboard.Url, nextElement: usernameCell.Entry);
            var nameCell = new FormEntryCell(AppResources.Name, nextElement: uriCell.Entry);

            var folderOptions = new List<string> { AppResources.FolderNone };
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy(f => f.Name?.Decrypt());
            foreach(var folder in folders)
            {
                folderOptions.Add(folder.Name.Decrypt());
            }
            var folderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());

            var generateCell = new ExtendedTextCell
            {
                Text = "Generate Password",
                ShowDisclousure = true
            };
            generateCell.Tapped += GenerateCell_Tapped; ;

            var favoriteCell = new ExtendedSwitchCell { Text = "Favorite" };

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection("Site Information")
                    {
                        nameCell,
                        uriCell,
                        usernameCell,
                        PasswordCell,
                        generateCell
                    },
                    new TableSection
                    {
                        folderCell,
                        favoriteCell
                    },
                    new TableSection(AppResources.Notes)
                    {
                        notesCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var saveToolBarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Password), AppResources.Ok);
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.Name), AppResources.Ok);
                    return;
                }

                var site = new Site
                {
                    Uri = uriCell.Entry.Text?.Encrypt(),
                    Name = nameCell.Entry.Text?.Encrypt(),
                    Username = usernameCell.Entry.Text?.Encrypt(),
                    Password = PasswordCell.Entry.Text?.Encrypt(),
                    Notes = notesCell.Editor.Text?.Encrypt(),
                    Favorite = favoriteCell.On
                };

                if(folderCell.Picker.SelectedIndex > 0)
                {
                    site.FolderId = folders.ElementAt(folderCell.Picker.SelectedIndex - 1).Id;
                }

                var saveTask = _siteService.SaveAsync(site);
                _userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                _userDialogs.HideLoading();
                await Navigation.PopModalAsync();
                _userDialogs.SuccessToast(nameCell.Entry.Text, "New site created.");
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.AddSite;
            Content = table;
            ToolbarItems.Add(saveToolBarItem);
            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel"));
            }

            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        private void GenerateCell_Tapped(object sender, EventArgs e)
        {
            var page = new ToolsPasswordGeneratorPage((password) =>
            {
                PasswordCell.Entry.Text = password;
                _userDialogs.SuccessToast("Password generated.");
            });
            Navigation.PushModalAsync(new ExtendedNavigationPage(page));
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
        }
    }
}

