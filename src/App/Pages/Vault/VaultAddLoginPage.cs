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
    public class VaultAddLoginPage : ExtendedContentPage
    {
        private const string AddedLoginAlertKey = "addedSiteAlert";

        private readonly ILoginService _loginService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly ISettings _settings;
        private readonly IAppInfoService _appInfoService;
        private readonly string _defaultUri;
        private readonly string _defaultName;
        private readonly bool _fromAutofill;

        public VaultAddLoginPage(string defaultUri = null, string defaultName = null, bool fromAutofill = false)
        {
            _defaultUri = defaultUri;
            _defaultName = defaultName;
            _fromAutofill = fromAutofill;

            _loginService = Resolver.Resolve<ILoginService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _settings = Resolver.Resolve<ISettings>();
            _appInfoService = Resolver.Resolve<IAppInfoService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; private set; }
        public FormEntryCell UsernameCell { get; private set; }
        public FormEntryCell UriCell { get; private set; }
        public FormEntryCell NameCell { get; private set; }
        public FormEditorCell NotesCell { get; private set; }
        public FormPickerCell FolderCell { get; private set; }
        public ExtendedTextCell GenerateCell { get; private set; }

        private void Init()
        {
            NotesCell = new FormEditorCell(height: 90);
            PasswordCell = new FormEntryCell(AppResources.Password, isPassword: true, nextElement: NotesCell.Editor,
                useButton: true);
            PasswordCell.Button.Image = "eye";
            PasswordCell.Entry.DisableAutocapitalize = true;
            PasswordCell.Entry.Autocorrect = false;
            PasswordCell.Entry.FontFamily = Device.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            UsernameCell = new FormEntryCell(AppResources.Username, nextElement: PasswordCell.Entry);
            UsernameCell.Entry.DisableAutocapitalize = true;
            UsernameCell.Entry.Autocorrect = false;

            UriCell = new FormEntryCell(AppResources.URI, Keyboard.Url, nextElement: UsernameCell.Entry);
            if(!string.IsNullOrWhiteSpace(_defaultUri))
            {
                UriCell.Entry.Text = _defaultUri;
            }

            NameCell = new FormEntryCell(AppResources.Name, nextElement: UriCell.Entry);
            if(!string.IsNullOrWhiteSpace(_defaultName))
            {
                NameCell.Entry.Text = _defaultName;
            }

            var folderOptions = new List<string> { AppResources.FolderNone };
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult()
                .OrderBy(f => f.Name?.Decrypt()).ToList();
            foreach(var folder in folders)
            {
                folderOptions.Add(folder.Name.Decrypt());
            }
            FolderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());

            GenerateCell = new ExtendedTextCell
            {
                Text = AppResources.GeneratePassword,
                ShowDisclousure = true
            };

            var favoriteCell = new ExtendedSwitchCell { Text = AppResources.Favorite };

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection(AppResources.LoginInformation)
                    {
                        NameCell,
                        UriCell,
                        UsernameCell,
                        PasswordCell,
                        GenerateCell
                    },
                    new TableSection(" ")
                    {
                        FolderCell,
                        favoriteCell
                    },
                    new TableSection(AppResources.Notes)
                    {
                        NotesCell
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

                if(string.IsNullOrWhiteSpace(NameCell.Entry.Text))
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                        AppResources.Name), AppResources.Ok);
                    return;
                }

                var login = new Login
                {
                    Uri = UriCell.Entry.Text?.Encrypt(),
                    Name = NameCell.Entry.Text?.Encrypt(),
                    Username = UsernameCell.Entry.Text?.Encrypt(),
                    Password = PasswordCell.Entry.Text?.Encrypt(),
                    Notes = NotesCell.Editor.Text?.Encrypt(),
                    Favorite = favoriteCell.On
                };

                if(FolderCell.Picker.SelectedIndex > 0)
                {
                    login.FolderId = folders.ElementAt(FolderCell.Picker.SelectedIndex - 1).Id;
                }

                _userDialogs.ShowLoading(AppResources.Saving, MaskType.Black);
                var saveTask = await _loginService.SaveAsync(login);

                _userDialogs.HideLoading();
                if(saveTask.Succeeded)
                {
                    await Navigation.PopForDeviceAsync();
                    _userDialogs.Toast(AppResources.NewLoginCreated);
                    if(_fromAutofill)
                    {
                        _googleAnalyticsService.TrackExtensionEvent("CreatedLogin");
                    }
                    else
                    {
                        _googleAnalyticsService.TrackAppEvent("CreatedLogin");
                    }
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

            Title = AppResources.AddLogin;
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

            PasswordCell.InitEvents();
            UsernameCell.InitEvents();
            UriCell.InitEvents();
            NameCell.InitEvents();
            NotesCell.InitEvents();
            FolderCell.InitEvents();
            PasswordCell.Button.Clicked += PasswordButton_Clicked;
            GenerateCell.Tapped += GenerateCell_Tapped;

            if(!_fromAutofill && !_settings.GetValueOrDefault(AddedLoginAlertKey, false))
            {
                _settings.AddOrUpdateValue(AddedLoginAlertKey, true);
                if(Device.OS == TargetPlatform.iOS)
                {
                    DisplayAlert(AppResources.BitwardenAppExtension, AppResources.BitwardenAppExtensionAlert,
                        AppResources.Ok);
                }
                else if(Device.OS == TargetPlatform.Android && !_appInfoService.AutofillServiceEnabled)
                {
                    DisplayAlert(AppResources.BitwardenAutofillService, AppResources.BitwardenAutofillServiceAlert,
                        AppResources.Ok);
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            PasswordCell.Dispose();
            UsernameCell.Dispose();
            UriCell.Dispose();
            NameCell.Dispose();
            NotesCell.Dispose();
            FolderCell.Dispose();
            PasswordCell.Button.Clicked -= PasswordButton_Clicked;
            GenerateCell.Tapped -= GenerateCell_Tapped;
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
            }, _fromAutofill);
            await Navigation.PushForDeviceAsync(page);
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }
    }
}

