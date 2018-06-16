using System;
using System.Collections.Generic;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Utilities;
using Bit.App.Enums;

namespace Bit.App.Pages
{
    public class VaultAddCipherPage : ExtendedContentPage
    {
        private const string AddedLoginAlertKey = "addedSiteAlert";

        private readonly CipherType _type;
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly IConnectivity _connectivity;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly ISettings _settings;
        private readonly IAppInfoService _appInfoService;
        private readonly IDeviceInfoService _deviceInfo;
        private readonly IDeviceActionService _deviceActionService;
        private readonly string _defaultFolderId;
        private readonly string _defaultUri;
        private readonly string _defaultName;
        private readonly string _defaultUsername;
        private readonly string _defaultPassword;
        private readonly string _defaultCardName;
        private readonly string _defaultCardNumber;
        private readonly int? _defaultCardExpMonth;
        private readonly string _defaultCardExpYear;
        private readonly string _defaultCardCode;
        private readonly bool _fromAutofill;
        private readonly bool _fromAutofillFramework;
        private DateTime? _lastAction;

        public VaultAddCipherPage(AppOptions options)
            : this(options.SaveType.Value, options.Uri, options.SaveName, options.FromAutofillFramework, false)
        {
            _fromAutofillFramework = options.FromAutofillFramework;
            _defaultUsername = options.SaveUsername;
            _defaultPassword = options.SavePassword;
            _defaultCardCode = options.SaveCardCode;
            if(int.TryParse(options.SaveCardExpMonth, out int month) && month <= 12 && month >= 1)
            {
                _defaultCardExpMonth = month;
            }
            _defaultCardExpYear = options.SaveCardExpYear;
            _defaultCardName = options.SaveCardName;
            _defaultCardNumber = options.SaveCardNumber;
            Init();
        }

        public VaultAddCipherPage(CipherType type, string defaultUri = null, string defaultName = null,
            bool fromAutofill = false, bool doInit = true, string defaultFolderId = null)
        {
            _defaultFolderId = defaultFolderId;
            _type = type;
            _defaultUri = defaultUri;
            _defaultName = defaultName;
            _fromAutofill = fromAutofill;

            _cipherService = Resolver.Resolve<ICipherService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _settings = Resolver.Resolve<ISettings>();
            _appInfoService = Resolver.Resolve<IAppInfoService>();
            _deviceInfo = Resolver.Resolve<IDeviceInfoService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();

            if(doInit)
            {
                Init();
            }
        }

        public List<Folder> Folders { get; set; }
        public TableRoot TableRoot { get; set; }
        public TableSection TopSection { get; set; }
        public TableSection UrisSection { get; set; }
        public TableSection MiddleSection { get; set; }
        public TableSection FieldsSection { get; set; }
        public ExtendedTableView Table { get; set; }

        public FormEntryCell NameCell { get; private set; }
        public FormEditorCell NotesCell { get; private set; }
        public FormPickerCell FolderCell { get; private set; }
        public ExtendedSwitchCell FavoriteCell { get; set; }
        public ExtendedTextCell AddFieldCell { get; private set; }
        public ExtendedTextCell AddUriCell { get; private set; }

        // Login
        public FormEntryCell LoginPasswordCell { get; private set; }
        public FormEntryCell LoginUsernameCell { get; private set; }
        public FormEntryCell LoginTotpCell { get; private set; }

        // Card
        public FormEntryCell CardNameCell { get; private set; }
        public FormEntryCell CardNumberCell { get; private set; }
        public FormPickerCell CardBrandCell { get; private set; }
        public FormPickerCell CardExpMonthCell { get; private set; }
        public FormEntryCell CardExpYearCell { get; private set; }
        public FormEntryCell CardCodeCell { get; private set; }

        // Identity
        public FormPickerCell IdTitleCell { get; private set; }
        public FormEntryCell IdFirstNameCell { get; private set; }
        public FormEntryCell IdMiddleNameCell { get; private set; }
        public FormEntryCell IdLastNameCell { get; private set; }
        public FormEntryCell IdUsernameCell { get; private set; }
        public FormEntryCell IdCompanyCell { get; private set; }
        public FormEntryCell IdSsnCell { get; private set; }
        public FormEntryCell IdPassportNumberCell { get; private set; }
        public FormEntryCell IdLicenseNumberCell { get; private set; }
        public FormEntryCell IdEmailCell { get; private set; }
        public FormEntryCell IdPhoneCell { get; private set; }
        public FormEntryCell IdAddress1Cell { get; private set; }
        public FormEntryCell IdAddress2Cell { get; private set; }
        public FormEntryCell IdAddress3Cell { get; private set; }
        public FormEntryCell IdCityCell { get; private set; }
        public FormEntryCell IdStateCell { get; private set; }
        public FormEntryCell IdPostalCodeCell { get; private set; }
        public FormEntryCell IdCountryCell { get; private set; }

        private void Init()
        {
            // Name
            NameCell = new FormEntryCell(AppResources.Name);
            if(!string.IsNullOrWhiteSpace(_defaultName))
            {
                NameCell.Entry.Text = _defaultName;
            }

            // Notes
            NotesCell = new FormEditorCell(Keyboard.Text, _type == CipherType.SecureNote ? 500 : 180);

            // Folders
            var folderOptions = new List<string> { AppResources.FolderNone };
            Folders = _folderService.GetAllAsync().GetAwaiter().GetResult()
                .OrderBy(f => f.Name?.Decrypt()).ToList();
            var selectedIndex = 0;
            var i = 1;
            foreach(var folder in Folders)
            {
                if(folder.Id == _defaultFolderId)
                {
                    selectedIndex = i;
                }
                folderOptions.Add(folder.Name.Decrypt());
                i++;
            }
            FolderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());
            FolderCell.Picker.SelectedIndex = selectedIndex;

            // Favorite
            FavoriteCell = new ExtendedSwitchCell { Text = AppResources.Favorite };

            InitTable();
            InitSave();

            Title = AppResources.AddItem;
            Content = Table;
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

            NameCell.InitEvents();
            NotesCell.InitEvents();
            FolderCell.InitEvents();

            if(AddFieldCell != null)
            {
                AddFieldCell.Tapped += AddFieldCell_Tapped;
            }
            if(AddUriCell != null)
            {
                AddUriCell.Tapped += AddUriCell_Tapped;
            }

            switch(_type)
            {
                case CipherType.Login:
                    LoginPasswordCell.InitEvents();
                    LoginUsernameCell.InitEvents();
                    LoginTotpCell.InitEvents();
                    LoginPasswordCell.Button1.Clicked += PasswordButton_Clicked;
                    LoginPasswordCell.Button2.Clicked += PasswordButton2_Clicked;
                    if(LoginTotpCell?.Button1 != null)
                    {
                        LoginTotpCell.Button1.Clicked += TotpButton_Clicked;
                    }
                    break;
                case CipherType.Card:
                    CardBrandCell.InitEvents();
                    CardCodeCell.InitEvents();
                    CardExpMonthCell.InitEvents();
                    CardExpYearCell.InitEvents();
                    CardNameCell.InitEvents();
                    CardNumberCell.InitEvents();
                    CardCodeCell.Button1.Clicked += CardCodeButton_Clicked;
                    break;
                case CipherType.Identity:
                    IdTitleCell.InitEvents();
                    IdFirstNameCell.InitEvents();
                    IdMiddleNameCell.InitEvents();
                    IdLastNameCell.InitEvents();
                    IdUsernameCell.InitEvents();
                    IdCompanyCell.InitEvents();
                    IdSsnCell.InitEvents();
                    IdPassportNumberCell.InitEvents();
                    IdLicenseNumberCell.InitEvents();
                    IdEmailCell.InitEvents();
                    IdPhoneCell.InitEvents();
                    IdAddress1Cell.InitEvents();
                    IdAddress2Cell.InitEvents();
                    IdAddress3Cell.InitEvents();
                    IdCityCell.InitEvents();
                    IdStateCell.InitEvents();
                    IdPostalCodeCell.InitEvents();
                    IdCountryCell.InitEvents();
                    break;
                default:
                    break;
            }

            Helpers.InitSectionEvents(FieldsSection);
            Helpers.InitSectionEvents(UrisSection);

            if(_type == CipherType.Login && !_fromAutofill && !_settings.GetValueOrDefault(AddedLoginAlertKey, false))
            {
                _settings.AddOrUpdateValue(AddedLoginAlertKey, true);
                if(Device.RuntimePlatform == Device.iOS)
                {
                    DisplayAlert(AppResources.BitwardenAppExtension, AppResources.BitwardenAppExtensionAlert,
                        AppResources.Ok);
                }
                else if(Device.RuntimePlatform == Device.Android && !_appInfoService.AutofillAccessibilityServiceEnabled)
                {
                    DisplayAlert(AppResources.BitwardenAutofillService, AppResources.BitwardenAutofillServiceAlert,
                        AppResources.Ok);
                }
            }

            if(NameCell != null && string.IsNullOrWhiteSpace(NameCell.Entry.Text))
            {
                NameCell.Entry.FocusWithDelay();
            }
            else if(LoginUsernameCell != null && string.IsNullOrWhiteSpace(LoginUsernameCell.Entry.Text))
            {
                LoginUsernameCell.Entry.FocusWithDelay();
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            NameCell.Dispose();
            NotesCell.Dispose();
            FolderCell.Dispose();

            if(AddFieldCell != null)
            {
                AddFieldCell.Tapped -= AddFieldCell_Tapped;
            }
            if(AddUriCell != null)
            {
                AddUriCell.Tapped -= AddUriCell_Tapped;
            }

            switch(_type)
            {
                case CipherType.Login:
                    LoginTotpCell.Dispose();
                    LoginPasswordCell.Dispose();
                    LoginUsernameCell.Dispose();
                    LoginPasswordCell.Button1.Clicked -= PasswordButton_Clicked;
                    LoginPasswordCell.Button2.Clicked -= PasswordButton2_Clicked;
                    if(LoginTotpCell?.Button1 != null)
                    {
                        LoginTotpCell.Button1.Clicked -= TotpButton_Clicked;
                    }
                    break;
                case CipherType.Card:
                    CardBrandCell.Dispose();
                    CardCodeCell.Dispose();
                    CardExpMonthCell.Dispose();
                    CardExpYearCell.Dispose();
                    CardNameCell.Dispose();
                    CardNumberCell.Dispose();
                    CardCodeCell.Button1.Clicked -= CardCodeButton_Clicked;
                    break;
                case CipherType.Identity:
                    IdTitleCell.Dispose();
                    IdFirstNameCell.Dispose();
                    IdMiddleNameCell.Dispose();
                    IdLastNameCell.Dispose();
                    IdUsernameCell.Dispose();
                    IdCompanyCell.Dispose();
                    IdSsnCell.Dispose();
                    IdPassportNumberCell.Dispose();
                    IdLicenseNumberCell.Dispose();
                    IdEmailCell.Dispose();
                    IdPhoneCell.Dispose();
                    IdAddress1Cell.Dispose();
                    IdAddress2Cell.Dispose();
                    IdAddress3Cell.Dispose();
                    IdCityCell.Dispose();
                    IdStateCell.Dispose();
                    IdPostalCodeCell.Dispose();
                    IdCountryCell.Dispose();
                    break;
                default:
                    break;
            }

            Helpers.DisposeSectionEvents(FieldsSection);
            Helpers.DisposeSectionEvents(UrisSection);
        }

        protected override bool OnBackButtonPressed()
        {
            if(_fromAutofillFramework)
            {
                Application.Current.MainPage = new MainPage();
                return true;
            }

            return base.OnBackButtonPressed();
        }

        private void PasswordButton_Clicked(object sender, EventArgs e)
        {
            LoginPasswordCell.Entry.InvokeToggleIsPassword();
            LoginPasswordCell.Button1.Image =
                "eye" + (!LoginPasswordCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty) + ".png";
        }

        private async void PasswordButton2_Clicked(object sender, EventArgs e)
        {
            var page = new ToolsPasswordGeneratorPage((password) =>
            {
                LoginPasswordCell.Entry.Text = password;
                _deviceActionService.Toast(AppResources.PasswordGenerated);
            }, _fromAutofill);
            await Navigation.PushForDeviceAsync(page);
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
                        LoginTotpCell.Entry.Text = key;
                        _deviceActionService.Toast(AppResources.AuthenticatorKeyAdded);
                    }
                    else
                    {
                        await DisplayAlert(null, AppResources.AuthenticatorKeyReadError, AppResources.Ok);
                    }
                });
            });

            await Navigation.PushModalAsync(new ExtendedNavigationPage(scanPage));
        }

        private void CardCodeButton_Clicked(object sender, EventArgs e)
        {
            CardCodeCell.Entry.InvokeToggleIsPassword();
            CardCodeCell.Button1.Image =
                "eye" + (!CardCodeCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty) + ".png";
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }

        private void InitTable()
        {
            // Sections
            TopSection = new TableSection(AppResources.ItemInformation)
            {
                NameCell
            };

            MiddleSection = new TableSection(Helpers.GetEmptyTableSectionTitle())
            {
                FolderCell,
                FavoriteCell
            };

            if(_type == CipherType.Login)
            {
                LoginTotpCell = new FormEntryCell(AppResources.AuthenticatorKey,
                    button1: _deviceInfo.HasCamera ? "camera.png" : null);
                LoginTotpCell.Entry.DisableAutocapitalize = true;
                LoginTotpCell.Entry.Autocorrect = false;
                LoginTotpCell.Entry.FontFamily =
                    Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");

                LoginPasswordCell = new FormEntryCell(AppResources.Password, isPassword: true, nextElement: LoginTotpCell.Entry,
                    button1: "eye.png", button2: "refresh_alt.png");
                LoginPasswordCell.Entry.DisableAutocapitalize = true;
                LoginPasswordCell.Entry.Autocorrect = false;
                LoginPasswordCell.Entry.FontFamily =
                    Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");
                if(!string.IsNullOrWhiteSpace(_defaultPassword))
                {
                    LoginPasswordCell.Entry.Text = _defaultPassword;
                }

                LoginUsernameCell = new FormEntryCell(AppResources.Username, nextElement: LoginPasswordCell.Entry);
                LoginUsernameCell.Entry.DisableAutocapitalize = true;
                LoginUsernameCell.Entry.Autocorrect = false;
                if(!string.IsNullOrWhiteSpace(_defaultUsername))
                {
                    LoginUsernameCell.Entry.Text = _defaultUsername;
                }

                NameCell.NextElement = LoginUsernameCell.Entry;

                // Build sections
                TopSection.Add(LoginUsernameCell);
                TopSection.Add(LoginPasswordCell);
                TopSection.Add(LoginTotpCell);

                // Uris
                UrisSection = new TableSection(Helpers.GetEmptyTableSectionTitle());
                AddUriCell = new ExtendedTextCell
                {
                    Text = $"+ {AppResources.NewUri}",
                    TextColor = Colors.Primary
                };
                UrisSection.Add(AddUriCell);
                UrisSection.Insert(0, Helpers.MakeUriCell(_defaultUri ?? string.Empty, null, UrisSection, this));
            }
            else if(_type == CipherType.Card)
            {
                CardCodeCell = new FormEntryCell(AppResources.SecurityCode, Keyboard.Numeric, isPassword: true, nextElement: NotesCell.Editor, button1: "eye.png");
                if(!string.IsNullOrWhiteSpace(_defaultCardCode))
                {
                    CardCodeCell.Entry.Text = _defaultCardCode;
                }
                CardExpYearCell = new FormEntryCell(AppResources.ExpirationYear, Keyboard.Numeric,
                    nextElement: CardCodeCell.Entry);
                if(!string.IsNullOrWhiteSpace(_defaultCardExpYear))
                {
                    CardExpYearCell.Entry.Text = _defaultCardExpYear;
                }
                CardExpMonthCell = new FormPickerCell(AppResources.ExpirationMonth, new string[] {
                    "--", AppResources.January, AppResources.February, AppResources.March, AppResources.April,
                    AppResources.May, AppResources.June, AppResources.July, AppResources.August, AppResources.September,
                    AppResources.October, AppResources.November, AppResources.December
                });
                if(_defaultCardExpMonth.HasValue)
                {
                    CardExpMonthCell.Picker.SelectedIndex = _defaultCardExpMonth.Value;
                }
                CardBrandCell = new FormPickerCell(AppResources.Brand, new string[] {
                    "--", "Visa", "Mastercard", "American Express", "Discover", "Diners Club",
                    "JCB", "Maestro", "UnionPay", AppResources.Other
                });
                CardNumberCell = new FormEntryCell(AppResources.Number, Keyboard.Numeric);
                if(!string.IsNullOrWhiteSpace(_defaultCardNumber))
                {
                    CardNumberCell.Entry.Text = _defaultCardNumber;
                }
                CardNameCell = new FormEntryCell(AppResources.CardholderName, nextElement: CardNumberCell.Entry);
                if(!string.IsNullOrWhiteSpace(_defaultCardName))
                {
                    CardNameCell.Entry.Text = _defaultCardName;
                }
                NameCell.NextElement = CardNameCell.Entry;

                // Build sections
                TopSection.Add(CardNameCell);
                TopSection.Add(CardNumberCell);
                TopSection.Add(CardBrandCell);
                TopSection.Add(CardExpMonthCell);
                TopSection.Add(CardExpYearCell);
                TopSection.Add(CardCodeCell);
            }
            else if(_type == CipherType.Identity)
            {
                IdCountryCell = new FormEntryCell(AppResources.Country, nextElement: NotesCell.Editor);
                IdPostalCodeCell = new FormEntryCell(AppResources.ZipPostalCode, nextElement: IdCountryCell.Entry);
                IdPostalCodeCell.Entry.DisableAutocapitalize = true;
                IdPostalCodeCell.Entry.Autocorrect = false;
                IdStateCell = new FormEntryCell(AppResources.StateProvince, nextElement: IdPostalCodeCell.Entry);
                IdCityCell = new FormEntryCell(AppResources.CityTown, nextElement: IdStateCell.Entry);
                IdAddress3Cell = new FormEntryCell(AppResources.Address3, nextElement: IdCityCell.Entry);
                IdAddress2Cell = new FormEntryCell(AppResources.Address2, nextElement: IdAddress3Cell.Entry);
                IdAddress1Cell = new FormEntryCell(AppResources.Address1, nextElement: IdAddress2Cell.Entry);
                IdPhoneCell = new FormEntryCell(AppResources.Phone, nextElement: IdAddress1Cell.Entry);
                IdPhoneCell.Entry.DisableAutocapitalize = true;
                IdPhoneCell.Entry.Autocorrect = false;
                IdEmailCell = new FormEntryCell(AppResources.Email, Keyboard.Email, nextElement: IdPhoneCell.Entry);
                IdEmailCell.Entry.DisableAutocapitalize = true;
                IdEmailCell.Entry.Autocorrect = false;
                IdLicenseNumberCell = new FormEntryCell(AppResources.LicenseNumber, nextElement: IdEmailCell.Entry);
                IdLicenseNumberCell.Entry.DisableAutocapitalize = true;
                IdLicenseNumberCell.Entry.Autocorrect = false;
                IdPassportNumberCell = new FormEntryCell(AppResources.PassportNumber, nextElement: IdLicenseNumberCell.Entry);
                IdPassportNumberCell.Entry.DisableAutocapitalize = true;
                IdPassportNumberCell.Entry.Autocorrect = false;
                IdSsnCell = new FormEntryCell(AppResources.SSN, nextElement: IdPassportNumberCell.Entry);
                IdSsnCell.Entry.DisableAutocapitalize = true;
                IdSsnCell.Entry.Autocorrect = false;
                IdCompanyCell = new FormEntryCell(AppResources.Company, nextElement: IdSsnCell.Entry);
                IdUsernameCell = new FormEntryCell(AppResources.Username, nextElement: IdCompanyCell.Entry);
                IdUsernameCell.Entry.DisableAutocapitalize = true;
                IdUsernameCell.Entry.Autocorrect = false;
                IdLastNameCell = new FormEntryCell(AppResources.LastName, nextElement: IdUsernameCell.Entry);
                IdMiddleNameCell = new FormEntryCell(AppResources.MiddleName, nextElement: IdLastNameCell.Entry);
                IdFirstNameCell = new FormEntryCell(AppResources.FirstName, nextElement: IdMiddleNameCell.Entry);
                IdTitleCell = new FormPickerCell(AppResources.Title, new string[] {
                    "--", AppResources.Mr, AppResources.Mrs, AppResources.Ms, AppResources.Dr
                });

                // Name
                NameCell.NextElement = IdFirstNameCell.Entry;

                // Build sections
                TopSection.Add(IdTitleCell);
                TopSection.Add(IdFirstNameCell);
                TopSection.Add(IdMiddleNameCell);
                TopSection.Add(IdLastNameCell);
                TopSection.Add(IdUsernameCell);
                TopSection.Add(IdCompanyCell);
                TopSection.Add(IdSsnCell);
                TopSection.Add(IdPassportNumberCell);
                TopSection.Add(IdLicenseNumberCell);
                TopSection.Add(IdEmailCell);
                TopSection.Add(IdPhoneCell);
                TopSection.Add(IdAddress1Cell);
                TopSection.Add(IdAddress2Cell);
                TopSection.Add(IdAddress3Cell);
                TopSection.Add(IdCityCell);
                TopSection.Add(IdStateCell);
                TopSection.Add(IdPostalCodeCell);
                TopSection.Add(IdCountryCell);
            }
            else if(_type == CipherType.SecureNote)
            {
                // Name
                NameCell.NextElement = NotesCell.Editor;
            }

            FieldsSection = new TableSection(AppResources.CustomFields);
            AddFieldCell = new ExtendedTextCell
            {
                Text = $"+ {AppResources.NewCustomField}",
                TextColor = Colors.Primary
            };
            FieldsSection.Add(AddFieldCell);

            // Make table
            TableRoot = new TableRoot
            {
                TopSection,
                MiddleSection,
                new TableSection(AppResources.Notes)
                {
                    NotesCell
                },
                FieldsSection
            };

            if(UrisSection != null)
            {
                TableRoot.Insert(1, UrisSection);
            }

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                Root = TableRoot
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 70;
            }
            else if(Device.RuntimePlatform == Device.Android)
            {
                Table.BottomPadding = 50;
            }
        }

        private void InitSave()
        {
            var saveToolBarItem = new ToolbarItem(AppResources.Save, Helpers.ToolbarImage("envelope.png"), async () =>
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

                var cipher = new Cipher
                {
                    Name = NameCell.Entry.Text.Encrypt(),
                    Notes = string.IsNullOrWhiteSpace(NotesCell.Editor.Text) ? null : NotesCell.Editor.Text.Encrypt(),
                    Favorite = FavoriteCell.On,
                    Type = _type
                };

                switch(_type)
                {
                    case CipherType.Login:
                        cipher.Login = new Login
                        {
                            Username = string.IsNullOrWhiteSpace(LoginUsernameCell.Entry.Text) ? null :
                                LoginUsernameCell.Entry.Text.Encrypt(),
                            Password = string.IsNullOrWhiteSpace(LoginPasswordCell.Entry.Text) ? null :
                                LoginPasswordCell.Entry.Text.Encrypt(),
                            Totp = string.IsNullOrWhiteSpace(LoginTotpCell.Entry.Text) ? null :
                                LoginTotpCell.Entry.Text.Encrypt(),
                        };

                        Helpers.ProcessUrisSectionForSave(UrisSection, cipher);
                        break;
                    case CipherType.SecureNote:
                        cipher.SecureNote = new SecureNote
                        {
                            Type = SecureNoteType.Generic
                        };
                        break;
                    case CipherType.Card:
                        string brand;
                        switch(CardBrandCell.Picker.SelectedIndex)
                        {
                            case 1:
                                brand = "Visa";
                                break;
                            case 2:
                                brand = "Mastercard";
                                break;
                            case 3:
                                brand = "Amex";
                                break;
                            case 4:
                                brand = "Discover";
                                break;
                            case 5:
                                brand = "Diners Club";
                                break;
                            case 6:
                                brand = "JCB";
                                break;
                            case 7:
                                brand = "Maestro";
                                break;
                            case 8:
                                brand = "UnionPay";
                                break;
                            case 9:
                                brand = "Other";
                                break;
                            default:
                                brand = null;
                                break;
                        }

                        var expMonth = CardExpMonthCell.Picker.SelectedIndex > 0 ?
                            CardExpMonthCell.Picker.SelectedIndex.ToString() : null;

                        cipher.Card = new Card
                        {
                            CardholderName = string.IsNullOrWhiteSpace(CardNameCell.Entry.Text) ? null :
                                CardNameCell.Entry.Text.Encrypt(),
                            Number = string.IsNullOrWhiteSpace(CardNumberCell.Entry.Text) ? null :
                                CardNumberCell.Entry.Text.Encrypt(),
                            ExpYear = string.IsNullOrWhiteSpace(CardExpYearCell.Entry.Text) ? null :
                                CardExpYearCell.Entry.Text.Encrypt(),
                            Code = string.IsNullOrWhiteSpace(CardCodeCell.Entry.Text) ? null :
                                CardCodeCell.Entry.Text.Encrypt(),
                            Brand = string.IsNullOrWhiteSpace(brand) ? null : brand.Encrypt(),
                            ExpMonth = string.IsNullOrWhiteSpace(expMonth) ? null : expMonth.Encrypt(),
                        };
                        break;
                    case CipherType.Identity:
                        string title;
                        switch(IdTitleCell.Picker.SelectedIndex)
                        {
                            case 1:
                                title = AppResources.Mr;
                                break;
                            case 2:
                                title = AppResources.Mrs;
                                break;
                            case 3:
                                title = AppResources.Ms;
                                break;
                            case 4:
                                title = AppResources.Dr;
                                break;
                            default:
                                title = null;
                                break;
                        }

                        cipher.Identity = new Identity
                        {
                            Title = string.IsNullOrWhiteSpace(title) ? null : title.Encrypt(),
                            FirstName = string.IsNullOrWhiteSpace(IdFirstNameCell.Entry.Text) ? null :
                                IdFirstNameCell.Entry.Text.Encrypt(),
                            MiddleName = string.IsNullOrWhiteSpace(IdMiddleNameCell.Entry.Text) ? null :
                                IdMiddleNameCell.Entry.Text.Encrypt(),
                            LastName = string.IsNullOrWhiteSpace(IdLastNameCell.Entry.Text) ? null :
                                IdLastNameCell.Entry.Text.Encrypt(),
                            Username = string.IsNullOrWhiteSpace(IdUsernameCell.Entry.Text) ? null :
                                IdUsernameCell.Entry.Text.Encrypt(),
                            Company = string.IsNullOrWhiteSpace(IdCompanyCell.Entry.Text) ? null :
                                IdCompanyCell.Entry.Text.Encrypt(),
                            SSN = string.IsNullOrWhiteSpace(IdSsnCell.Entry.Text) ? null :
                                IdSsnCell.Entry.Text.Encrypt(),
                            PassportNumber = string.IsNullOrWhiteSpace(IdPassportNumberCell.Entry.Text) ? null :
                                IdPassportNumberCell.Entry.Text.Encrypt(),
                            LicenseNumber = string.IsNullOrWhiteSpace(IdLicenseNumberCell.Entry.Text) ? null :
                                IdLicenseNumberCell.Entry.Text.Encrypt(),
                            Email = string.IsNullOrWhiteSpace(IdEmailCell.Entry.Text) ? null :
                                IdEmailCell.Entry.Text.Encrypt(),
                            Phone = string.IsNullOrWhiteSpace(IdPhoneCell.Entry.Text) ? null :
                                IdPhoneCell.Entry.Text.Encrypt(),
                            Address1 = string.IsNullOrWhiteSpace(IdAddress1Cell.Entry.Text) ? null :
                                IdAddress1Cell.Entry.Text.Encrypt(),
                            Address2 = string.IsNullOrWhiteSpace(IdAddress2Cell.Entry.Text) ? null :
                                IdAddress2Cell.Entry.Text.Encrypt(),
                            Address3 = string.IsNullOrWhiteSpace(IdAddress3Cell.Entry.Text) ? null :
                                IdAddress3Cell.Entry.Text.Encrypt(),
                            City = string.IsNullOrWhiteSpace(IdCityCell.Entry.Text) ? null :
                                IdCityCell.Entry.Text.Encrypt(),
                            State = string.IsNullOrWhiteSpace(IdStateCell.Entry.Text) ? null :
                                IdStateCell.Entry.Text.Encrypt(),
                            PostalCode = string.IsNullOrWhiteSpace(IdPostalCodeCell.Entry.Text) ? null :
                                IdPostalCodeCell.Entry.Text.Encrypt(),
                            Country = string.IsNullOrWhiteSpace(IdCountryCell.Entry.Text) ? null :
                                IdCountryCell.Entry.Text.Encrypt()
                        };
                        break;
                    default:
                        break;
                }

                if(FolderCell.Picker.SelectedIndex > 0)
                {
                    cipher.FolderId = Folders.ElementAt(FolderCell.Picker.SelectedIndex - 1).Id;
                }

                Helpers.ProcessFieldsSectionForSave(FieldsSection, cipher);

                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                var saveTask = await _cipherService.SaveAsync(cipher);
                await _deviceActionService.HideLoadingAsync();

                if(saveTask.Succeeded)
                {
                    _deviceActionService.Toast(AppResources.NewItemCreated);
                    if(_fromAutofill)
                    {
                        _googleAnalyticsService.TrackExtensionEvent("CreatedCipher");
                    }
                    else
                    {
                        _googleAnalyticsService.TrackAppEvent("CreatedCipher");
                    }

                    if(_fromAutofillFramework)
                    {
                        // close and go back to app
                        _deviceActionService.CloseAutofill();
                    }
                    else
                    {
                        await Navigation.PopForDeviceAsync();
                    }
                }
                else if(saveTask.Errors.Count() > 0)
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, saveTask.Errors.First().Message, AppResources.Ok);
                }
                else
                {
                    await DisplayAlert(null, AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(saveToolBarItem);
        }

        private async void AddFieldCell_Tapped(object sender, EventArgs e)
        {
            await Helpers.AddField(this, FieldsSection);
        }

        private void AddUriCell_Tapped(object sender, EventArgs e)
        {
            var cell = Helpers.MakeUriCell(string.Empty, null, UrisSection, this);
            if(cell != null)
            {
                UrisSection.Insert(UrisSection.Count - 1, cell);
                cell.InitEvents();
            }
        }
    }
}
