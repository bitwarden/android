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
using Bit.App.Utilities;

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
        private readonly IDeviceInfoService _deviceInfo;
        private readonly string _defaultUri;
        private readonly string _defaultName;
        private readonly bool _fromAutofill;
        private DateTime? _lastAction;

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
            _deviceInfo = Resolver.Resolve<IDeviceInfoService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; private set; }
        public FormEntryCell UsernameCell { get; private set; }
        public FormEntryCell UriCell { get; private set; }
        public FormEntryCell NameCell { get; private set; }
        public FormEntryCell TotpCell { get; private set; }
        public FormEditorCell NotesCell { get; private set; }
        public FormPickerCell FolderCell { get; private set; }
        public ExtendedTextCell GenerateCell { get; private set; }

        private void Init()
        {
            NotesCell = new FormEditorCell(height: 180);

            TotpCell = new FormEntryCell(AppResources.AuthenticatorKey, nextElement: NotesCell.Editor,
                useButton: _deviceInfo.HasCamera);
            if(_deviceInfo.HasCamera)
            {
                TotpCell.Button.Image = "camera";
            }
            TotpCell.Entry.DisableAutocapitalize = true;
            TotpCell.Entry.Autocorrect = false;
            TotpCell.Entry.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier");

            PasswordCell = new FormEntryCell(AppResources.Password, isPassword: true, nextElement: TotpCell.Entry,
                useButton: true);
            PasswordCell.Button.Image = "eye";
            PasswordCell.Entry.DisableAutocapitalize = true;
            PasswordCell.Entry.Autocorrect = false;
            PasswordCell.Entry.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier");

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
                        TotpCell,
                        FolderCell,
                        favoriteCell
                    },
                    new TableSection(AppResources.Notes)
                    {
                        NotesCell
                    }
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }
            else if(Device.RuntimePlatform == Device.Android)
            {
                PasswordCell.Button.WidthRequest = 40;

                if(TotpCell.Button != null)
                {
                    TotpCell.Button.WidthRequest = 40;
                }
            }

            var saveToolBarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(_lastAction.LastActionWasRecent())
                {
                    return;
                }
                _lastAction = DateTime.UtcNow;

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
                    Name = NameCell.Entry.Text.Encrypt(),
                    Uri = string.IsNullOrWhiteSpace(UriCell.Entry.Text) ? null : UriCell.Entry.Text.Encrypt(),
                    Username = string.IsNullOrWhiteSpace(UsernameCell.Entry.Text) ? null : UsernameCell.Entry.Text.Encrypt(),
                    Password = string.IsNullOrWhiteSpace(PasswordCell.Entry.Text) ? null : PasswordCell.Entry.Text.Encrypt(),
                    Notes = string.IsNullOrWhiteSpace(NotesCell.Editor.Text) ? null : NotesCell.Editor.Text.Encrypt(),
                    Totp = string.IsNullOrWhiteSpace(TotpCell.Entry.Text) ? null : TotpCell.Entry.Text.Encrypt(),
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
                    _userDialogs.Toast(AppResources.NewLoginCreated);
                    if(_fromAutofill)
                    {
                        _googleAnalyticsService.TrackExtensionEvent("CreatedLogin");
                    }
                    else
                    {
                        _googleAnalyticsService.TrackAppEvent("CreatedLogin");
                    }
                    await Navigation.PopForDeviceAsync();
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
            if(Device.RuntimePlatform == Device.iOS)
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
            TotpCell.InitEvents();
            FolderCell.InitEvents();
            PasswordCell.Button.Clicked += PasswordButton_Clicked;
            if(TotpCell?.Button != null)
            {
                TotpCell.Button.Clicked += TotpButton_Clicked;
            }
            GenerateCell.Tapped += GenerateCell_Tapped;

            if(!_fromAutofill && !_settings.GetValueOrDefault(AddedLoginAlertKey, false))
            {
                _settings.AddOrUpdateValue(AddedLoginAlertKey, true);
                if(Device.RuntimePlatform == Device.iOS)
                {
                    DisplayAlert(AppResources.BitwardenAppExtension, AppResources.BitwardenAppExtensionAlert,
                        AppResources.Ok);
                }
                else if(Device.RuntimePlatform == Device.Android && !_appInfoService.AutofillServiceEnabled)
                {
                    DisplayAlert(AppResources.BitwardenAutofillService, AppResources.BitwardenAutofillServiceAlert,
                        AppResources.Ok);
                }
            }

            NameCell.Entry.FocusWithDelay();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            PasswordCell.Dispose();
            UsernameCell.Dispose();
            UriCell.Dispose();
            NameCell.Dispose();
            NotesCell.Dispose();
            TotpCell.Dispose();
            FolderCell.Dispose();
            PasswordCell.Button.Clicked -= PasswordButton_Clicked;
            if(TotpCell?.Button != null)
            {
                TotpCell.Button.Clicked -= TotpButton_Clicked;
            }
            GenerateCell.Tapped -= GenerateCell_Tapped;
        }

        private void PasswordButton_Clicked(object sender, EventArgs e)
        {
            PasswordCell.Entry.InvokeToggleIsPassword();
            PasswordCell.Button.Image = "eye" + (!PasswordCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty);
        }

        private async void TotpButton_Clicked(object sender, EventArgs e)
        {
            var scanPage = new ScanPage((key) =>
            {
                Device.BeginInvokeOnMainThread(async () =>
                {
                    await Navigation.PopModalAsync();
                    if(!string.IsNullOrWhiteSpace(key))
                    {
                        TotpCell.Entry.Text = key;
                        _userDialogs.Toast(AppResources.AuthenticatorKeyAdded);
                    }
                    else
                    {
                        _userDialogs.Alert(AppResources.AuthenticatorKeyReadError);
                    }
                });
            });

            await Navigation.PushModalAsync(new ExtendedNavigationPage(scanPage));
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

