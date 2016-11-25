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
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class VaultAddSitePage : ExtendedContentPage
    {
        private const string AddedSiteAlertKey = "addedSiteAlert";

        private readonly ISiteService _siteService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly ISettings _settings;

        public VaultAddSitePage()
        {
            _siteService = Resolver.Resolve<ISiteService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public FormEntryCell PasswordCell { get; private set; }

        private void Init()
        {
            var notesCell = new FormEditorCell(height: 90);
            PasswordCell = new FormEntryCell(AppResources.Password, isPassword: true, nextElement: notesCell.Editor,
                useButton: true);
            PasswordCell.Button.Image = "eye";
            PasswordCell.Button.Clicked += PasswordButton_Clicked;
            PasswordCell.Entry.DisableAutocapitalize = true;
            PasswordCell.Entry.Autocorrect = false;
            PasswordCell.Entry.FontFamily = Device.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            var usernameCell = new FormEntryCell(AppResources.Username, nextElement: PasswordCell.Entry);
            usernameCell.Entry.DisableAutocapitalize = true;
            usernameCell.Entry.Autocorrect = false;

            var uriCell = new FormEntryCell(AppResources.URI, Keyboard.Url, nextElement: usernameCell.Entry);
            var nameCell = new FormEntryCell(AppResources.Name, nextElement: uriCell.Entry);

            var folderOptions = new List<string> { AppResources.FolderNone };
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult()
                .OrderBy(f => f.Name?.Decrypt()).ToList();
            foreach(var folder in folders)
            {
                folderOptions.Add(folder.Name.Decrypt());
            }
            var folderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());

            var generateCell = new ExtendedTextCell
            {
                Text = AppResources.GeneratePassword,
                ShowDisclousure = true
            };
            generateCell.Tapped += GenerateCell_Tapped; ;

            var favoriteCell = new ExtendedSwitchCell { Text = AppResources.Favorite };

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection(AppResources.SiteInformation)
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
            else if(Device.OS == TargetPlatform.Android)
            {
                PasswordCell.Button.WidthRequest = 40;
            }

            var saveToolBarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                        AppResources.Name), AppResources.Ok);
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

                _userDialogs.ShowLoading(AppResources.Saving, MaskType.Black);
                var saveTask = await _siteService.SaveAsync(site);

                _userDialogs.HideLoading();
                if(saveTask.Succeeded)
                {
                    await Navigation.PopForDeviceAsync();
                    _userDialogs.Toast(AppResources.NewSiteCreated);
                    _googleAnalyticsService.TrackAppEvent("CreatedSite");
                }
                else if(saveTask.Errors.Count() > 0)
                {
                    await _userDialogs.AlertAsync(saveTask.Errors.First().Message, AppResources.AnErrorHasOccurred);
                }
                else
                {
                    await _userDialogs.AlertAsync(AppResources.AnErrorHasOccurred);
                }
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.AddSite;
            Content = table;
            ToolbarItems.Add(saveToolBarItem);
            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }

            if(Device.OS == TargetPlatform.iOS && !_settings.GetValueOrDefault(AddedSiteAlertKey, false))
            {
                _settings.AddOrUpdateValue(AddedSiteAlertKey, true);
                DisplayAlert(AppResources.BitwardenAppExtension, AppResources.BitwardenAppExtensionAlert, AppResources.Ok);
            }
        }

        private void PasswordButton_Clicked(object sender, EventArgs e)
        {
            PasswordCell.Entry.InvokeToggleIsPassword();
            PasswordCell.Button.Image = "eye" + (!PasswordCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty);
        }

        private async void GenerateCell_Tapped(object sender, EventArgs e)
        {
            var page = new ToolsPasswordGeneratorPage((password) =>
            {
                PasswordCell.Entry.Text = password;
                _userDialogs.Toast(AppResources.PasswordGenerated);
            });
            await Navigation.PushForDeviceAsync(page);
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }
    }
}

