using System;
using System.Collections.Generic;
using System.Linq;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Utilities;
using Bit.App.Models;
using Bit.App.Enums;

namespace Bit.App.Pages
{
    public class VaultEditCipherPage : ExtendedContentPage
    {
        private readonly string _cipherId;
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IConnectivity _connectivity;
        private readonly IDeviceInfoService _deviceInfo;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private DateTime? _lastAction;

        public VaultEditCipherPage(string cipherId)
        {
            _cipherId = cipherId;
            _cipherService = Resolver.Resolve<ICipherService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _deviceInfo = Resolver.Resolve<IDeviceInfoService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public Cipher Cipher { get; set; }
        public List<Folder> Folders { get; set; }
        public TableRoot TableRoot { get; set; }
        public TableSection TopSection { get; set; }
        public TableSection MiddleSection { get; set; }
        public ExtendedTableView Table { get; set; }

        public FormEntryCell NameCell { get; private set; }
        public FormEditorCell NotesCell { get; private set; }
        public FormPickerCell FolderCell { get; private set; }
        public ExtendedSwitchCell FavoriteCell { get; set; }
        public ExtendedTextCell AttachmentsCell { get; private set; }
        public ExtendedTextCell CustomFieldsCell { get; private set; }
        public ExtendedTextCell DeleteCell { get; private set; }

        // Login
        public FormEntryCell LoginPasswordCell { get; private set; }
        public FormEntryCell LoginUsernameCell { get; private set; }
        public FormEntryCell LoginUriCell { get; private set; }
        public FormEntryCell LoginTotpCell { get; private set; }
        public ExtendedTextCell LoginGenerateCell { get; private set; }

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
            Cipher = _cipherService.GetByIdAsync(_cipherId).GetAwaiter().GetResult();
            if(Cipher == null)
            {
                // TODO: handle error. navigate back? should never happen...
                return;
            }

            // Name
            NameCell = new FormEntryCell(AppResources.Name);
            NameCell.Entry.Text = Cipher.Name?.Decrypt(Cipher.OrganizationId);

            // Notes
            NotesCell = new FormEditorCell(Keyboard.Text, Cipher.Type == CipherType.SecureNote ? 500 : 180);
            NotesCell.Editor.Text = Cipher.Notes?.Decrypt(Cipher.OrganizationId);

            // Folders
            var folderOptions = new List<string> { AppResources.FolderNone };
            Folders = _folderService.GetAllAsync().GetAwaiter().GetResult()
                .OrderBy(f => f.Name?.Decrypt()).ToList();
            int selectedIndex = 0;
            int i = 0;
            foreach(var folder in Folders)
            {
                i++;
                if(folder.Id == Cipher.FolderId)
                {
                    selectedIndex = i;
                }

                folderOptions.Add(folder.Name.Decrypt());
            }
            FolderCell = new FormPickerCell(AppResources.Folder, folderOptions.ToArray());
            FolderCell.Picker.SelectedIndex = selectedIndex;

            // Favorite
            FavoriteCell = new ExtendedSwitchCell
            {
                Text = AppResources.Favorite,
                On = Cipher.Favorite
            };

            // Delete
            DeleteCell = new ExtendedTextCell { Text = AppResources.Delete, TextColor = Color.Red };

            InitTable();
            InitSave();

            Title = AppResources.EditItem;
            Content = Table;
            if(Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
            }
        }

        private void InitTable()
        {
            AttachmentsCell = new ExtendedTextCell
            {
                Text = AppResources.Attachments,
                ShowDisclousure = true
            };

            CustomFieldsCell = new ExtendedTextCell
            {
                Text = AppResources.CustomFields,
                ShowDisclousure = true
            };

            // Sections
            TopSection = new TableSection(AppResources.ItemInformation)
            {
                NameCell
            };

            MiddleSection = new TableSection(Helpers.GetEmptyTableSectionTitle())
            {
                FolderCell,
                FavoriteCell,
                AttachmentsCell,
                CustomFieldsCell
            };

            // Types
            if(Cipher.Type == CipherType.Login)
            {
                LoginTotpCell = new FormEntryCell(AppResources.AuthenticatorKey, nextElement: NotesCell.Editor,
                    useButton: _deviceInfo.HasCamera);
                if(_deviceInfo.HasCamera)
                {
                    LoginTotpCell.Button.Image = "camera.png";
                }
                LoginTotpCell.Entry.Text = Cipher.Login?.Totp?.Decrypt(Cipher.OrganizationId);
                LoginTotpCell.Entry.DisableAutocapitalize = true;
                LoginTotpCell.Entry.Autocorrect = false;
                LoginTotpCell.Entry.FontFamily =
                    Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");

                LoginPasswordCell = new FormEntryCell(AppResources.Password, isPassword: true,
                    nextElement: LoginTotpCell.Entry, useButton: true);
                LoginPasswordCell.Entry.Text = Cipher.Login?.Password?.Decrypt(Cipher.OrganizationId);
                LoginPasswordCell.Button.Image = "eye.png";
                LoginPasswordCell.Entry.DisableAutocapitalize = true;
                LoginPasswordCell.Entry.Autocorrect = false;
                LoginPasswordCell.Entry.FontFamily =
                    Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");

                LoginGenerateCell = new ExtendedTextCell
                {
                    Text = AppResources.GeneratePassword,
                    ShowDisclousure = true
                };

                LoginUsernameCell = new FormEntryCell(AppResources.Username, nextElement: LoginPasswordCell.Entry);
                LoginUsernameCell.Entry.Text = Cipher.Login?.Username?.Decrypt(Cipher.OrganizationId);
                LoginUsernameCell.Entry.DisableAutocapitalize = true;
                LoginUsernameCell.Entry.Autocorrect = false;

                LoginUriCell = new FormEntryCell(AppResources.URI, Keyboard.Url, nextElement: LoginUsernameCell.Entry);
                LoginUriCell.Entry.Text = Cipher.Login?.Uri?.Decrypt(Cipher.OrganizationId);

                // Name
                NameCell.NextElement = LoginUriCell.Entry;

                // Build sections
                TopSection.Add(LoginUriCell);
                TopSection.Add(LoginUsernameCell);
                TopSection.Add(LoginPasswordCell);
                TopSection.Add(LoginGenerateCell);
                MiddleSection.Insert(0, LoginTotpCell);
            }
            else if(Cipher.Type == CipherType.Card)
            {
                CardCodeCell = new FormEntryCell(AppResources.SecurityCode, Keyboard.Numeric,
                    nextElement: NotesCell.Editor);
                CardCodeCell.Entry.Text = Cipher.Card.Code?.Decrypt(Cipher.OrganizationId);

                CardExpYearCell = new FormEntryCell(AppResources.ExpirationYear, Keyboard.Numeric,
                    nextElement: CardCodeCell.Entry);
                CardExpYearCell.Entry.Text = Cipher.Card.ExpYear?.Decrypt(Cipher.OrganizationId);

                var month = Cipher.Card.ExpMonth?.Decrypt(Cipher.OrganizationId);
                CardExpMonthCell = new FormPickerCell(AppResources.ExpirationMonth, new string[] {
                    "--", AppResources.January, AppResources.February, AppResources.March, AppResources.April,
                    AppResources.May, AppResources.June, AppResources.July, AppResources.August, AppResources.September,
                    AppResources.October, AppResources.November, AppResources.December
                });
                if(!string.IsNullOrWhiteSpace(month) && int.TryParse(month, out int monthIndex))
                {
                    CardExpMonthCell.Picker.SelectedIndex = monthIndex;
                }
                else
                {
                    CardExpMonthCell.Picker.SelectedIndex = 0;
                }

                var brandOptions = new string[] {
                    "--", "Visa", "Mastercard", "American Express", "Discover", "Diners Club",
                    "JCB", "Maestro", "UnionPay", AppResources.Other
                };
                var brand = Cipher.Card.Brand?.Decrypt(Cipher.OrganizationId);
                CardBrandCell = new FormPickerCell(AppResources.Brand, brandOptions);
                CardBrandCell.Picker.SelectedIndex = 0;
                if(!string.IsNullOrWhiteSpace(brand))
                {
                    var i = 0;
                    foreach(var o in brandOptions)
                    {
                        var option = o;
                        if(option == AppResources.Other)
                        {
                            option = "Other";
                        }

                        if(option == brand)
                        {
                            CardBrandCell.Picker.SelectedIndex = i;
                            break;
                        }
                        i++;
                    }
                }

                CardNumberCell = new FormEntryCell(AppResources.Number, Keyboard.Numeric);
                CardNumberCell.Entry.Text = Cipher.Card.Number?.Decrypt(Cipher.OrganizationId);

                CardNameCell = new FormEntryCell(AppResources.CardholderName, nextElement: CardNumberCell.Entry);
                CardNameCell.Entry.Text = Cipher.Card.CardholderName?.Decrypt(Cipher.OrganizationId);

                // Name
                NameCell.NextElement = CardNameCell.Entry;

                // Build sections
                TopSection.Add(CardNameCell);
                TopSection.Add(CardNumberCell);
                TopSection.Add(CardBrandCell);
                TopSection.Add(CardExpMonthCell);
                TopSection.Add(CardExpYearCell);
                TopSection.Add(CardCodeCell);
            }
            else if(Cipher.Type == CipherType.Identity)
            {
                IdCountryCell = new FormEntryCell(AppResources.Country, nextElement: NotesCell.Editor);
                IdCountryCell.Entry.Text = Cipher.Identity.Country?.Decrypt(Cipher.OrganizationId);

                IdPostalCodeCell = new FormEntryCell(AppResources.ZipPostalCode, nextElement: IdCountryCell.Entry);
                IdPostalCodeCell.Entry.Text = Cipher.Identity.PostalCode?.Decrypt(Cipher.OrganizationId);
                IdPostalCodeCell.Entry.DisableAutocapitalize = true;
                IdPostalCodeCell.Entry.Autocorrect = false;

                IdStateCell = new FormEntryCell(AppResources.StateProvince, nextElement: IdPostalCodeCell.Entry);
                IdStateCell.Entry.Text = Cipher.Identity.State?.Decrypt(Cipher.OrganizationId);

                IdCityCell = new FormEntryCell(AppResources.CityTown, nextElement: IdStateCell.Entry);
                IdCityCell.Entry.Text = Cipher.Identity.City?.Decrypt(Cipher.OrganizationId);

                IdAddress3Cell = new FormEntryCell(AppResources.Address3, nextElement: IdCityCell.Entry);
                IdAddress3Cell.Entry.Text = Cipher.Identity.Address3?.Decrypt(Cipher.OrganizationId);

                IdAddress2Cell = new FormEntryCell(AppResources.Address2, nextElement: IdAddress3Cell.Entry);
                IdAddress2Cell.Entry.Text = Cipher.Identity.Address2?.Decrypt(Cipher.OrganizationId);

                IdAddress1Cell = new FormEntryCell(AppResources.Address1, nextElement: IdAddress2Cell.Entry);
                IdAddress1Cell.Entry.Text = Cipher.Identity.Address1?.Decrypt(Cipher.OrganizationId);

                IdPhoneCell = new FormEntryCell(AppResources.Phone, nextElement: IdAddress1Cell.Entry);
                IdPhoneCell.Entry.Text = Cipher.Identity.Phone?.Decrypt(Cipher.OrganizationId);
                IdPhoneCell.Entry.DisableAutocapitalize = true;
                IdPhoneCell.Entry.Autocorrect = false;

                IdEmailCell = new FormEntryCell(AppResources.Email, Keyboard.Email, nextElement: IdPhoneCell.Entry);
                IdEmailCell.Entry.Text = Cipher.Identity.Email?.Decrypt(Cipher.OrganizationId);
                IdEmailCell.Entry.DisableAutocapitalize = true;
                IdEmailCell.Entry.Autocorrect = false;

                IdLicenseNumberCell = new FormEntryCell(AppResources.LicenseNumber, nextElement: IdEmailCell.Entry);
                IdLicenseNumberCell.Entry.Text = Cipher.Identity.LicenseNumber?.Decrypt(Cipher.OrganizationId);
                IdLicenseNumberCell.Entry.DisableAutocapitalize = true;
                IdLicenseNumberCell.Entry.Autocorrect = false;

                IdPassportNumberCell = new FormEntryCell(AppResources.PassportNumber, nextElement: IdLicenseNumberCell.Entry);
                IdPassportNumberCell.Entry.Text = Cipher.Identity.PassportNumber?.Decrypt(Cipher.OrganizationId);
                IdPassportNumberCell.Entry.DisableAutocapitalize = true;
                IdPassportNumberCell.Entry.Autocorrect = false;

                IdSsnCell = new FormEntryCell(AppResources.SSN, nextElement: IdPassportNumberCell.Entry);
                IdSsnCell.Entry.Text = Cipher.Identity.SSN?.Decrypt(Cipher.OrganizationId);
                IdSsnCell.Entry.DisableAutocapitalize = true;
                IdSsnCell.Entry.Autocorrect = false;

                IdCompanyCell = new FormEntryCell(AppResources.Company, nextElement: IdSsnCell.Entry);
                IdCompanyCell.Entry.Text = Cipher.Identity.Company?.Decrypt(Cipher.OrganizationId);

                IdUsernameCell = new FormEntryCell(AppResources.Username, nextElement: IdCompanyCell.Entry);
                IdUsernameCell.Entry.Text = Cipher.Identity.Username?.Decrypt(Cipher.OrganizationId);
                IdUsernameCell.Entry.DisableAutocapitalize = true;
                IdUsernameCell.Entry.Autocorrect = false;

                IdLastNameCell = new FormEntryCell(AppResources.LastName, nextElement: IdUsernameCell.Entry);
                IdLastNameCell.Entry.Text = Cipher.Identity.LastName?.Decrypt(Cipher.OrganizationId);

                IdMiddleNameCell = new FormEntryCell(AppResources.MiddleName, nextElement: IdLastNameCell.Entry);
                IdMiddleNameCell.Entry.Text = Cipher.Identity.MiddleName?.Decrypt(Cipher.OrganizationId);

                IdFirstNameCell = new FormEntryCell(AppResources.FirstName, nextElement: IdMiddleNameCell.Entry);
                IdFirstNameCell.Entry.Text = Cipher.Identity.FirstName?.Decrypt(Cipher.OrganizationId);

                var titleOptions = new string[] {
                    "--", AppResources.Mr, AppResources.Mrs, AppResources.Ms, AppResources.Dr
                };
                IdTitleCell = new FormPickerCell(AppResources.Title, titleOptions);
                var title = Cipher.Identity.Title?.Decrypt(Cipher.OrganizationId);
                IdTitleCell.Picker.SelectedIndex = 0;
                if(!string.IsNullOrWhiteSpace(title))
                {
                    var i = 0;
                    foreach(var o in titleOptions)
                    {
                        i++;
                        if(o == title)
                        {
                            IdTitleCell.Picker.SelectedIndex = i;
                            break;
                        }
                    }
                }

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
            else if(Cipher.Type == CipherType.SecureNote)
            {
                // Name
                NameCell.NextElement = NotesCell.Editor;
            }

            // Make table
            TableRoot = new TableRoot
            {
                TopSection,
                MiddleSection,
                new TableSection(AppResources.Notes)
                {
                    NotesCell
                },
                new TableSection(Helpers.GetEmptyTableSectionTitle())
                {
                    DeleteCell
                }
            };

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

                Cipher.Name = NameCell.Entry.Text.Encrypt(Cipher.OrganizationId);
                Cipher.Notes = string.IsNullOrWhiteSpace(NotesCell.Editor.Text) ? null :
                    NotesCell.Editor.Text.Encrypt(Cipher.OrganizationId);
                Cipher.Favorite = FavoriteCell.On;

                switch(Cipher.Type)
                {
                    case CipherType.Login:
                        Cipher.Login = new Login
                        {
                            Uri = string.IsNullOrWhiteSpace(LoginUriCell.Entry.Text) ? null :
                                LoginUriCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Username = string.IsNullOrWhiteSpace(LoginUsernameCell.Entry.Text) ? null :
                                LoginUsernameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Password = string.IsNullOrWhiteSpace(LoginPasswordCell.Entry.Text) ? null :
                                LoginPasswordCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Totp = string.IsNullOrWhiteSpace(LoginTotpCell.Entry.Text) ? null :
                                LoginTotpCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                        };
                        break;
                    case CipherType.SecureNote:
                        Cipher.SecureNote = new SecureNote
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

                        Cipher.Card = new Card
                        {
                            CardholderName = string.IsNullOrWhiteSpace(CardNameCell.Entry.Text) ? null :
                                CardNameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Number = string.IsNullOrWhiteSpace(CardNumberCell.Entry.Text) ? null :
                                CardNumberCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            ExpYear = string.IsNullOrWhiteSpace(CardExpYearCell.Entry.Text) ? null :
                                CardExpYearCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Code = string.IsNullOrWhiteSpace(CardCodeCell.Entry.Text) ? null :
                                CardCodeCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Brand = string.IsNullOrWhiteSpace(brand) ? null : brand.Encrypt(Cipher.OrganizationId),
                            ExpMonth = string.IsNullOrWhiteSpace(expMonth) ? null : expMonth.Encrypt(Cipher.OrganizationId)
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

                        Cipher.Identity = new Identity
                        {
                            Title = string.IsNullOrWhiteSpace(title) ? null : title.Encrypt(Cipher.OrganizationId),
                            FirstName = string.IsNullOrWhiteSpace(IdFirstNameCell.Entry.Text) ? null :
                                IdFirstNameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            MiddleName = string.IsNullOrWhiteSpace(IdMiddleNameCell.Entry.Text) ? null :
                                IdMiddleNameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            LastName = string.IsNullOrWhiteSpace(IdLastNameCell.Entry.Text) ? null :
                                IdLastNameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Username = string.IsNullOrWhiteSpace(IdUsernameCell.Entry.Text) ? null :
                                IdUsernameCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Company = string.IsNullOrWhiteSpace(IdCompanyCell.Entry.Text) ? null :
                                IdCompanyCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            SSN = string.IsNullOrWhiteSpace(IdSsnCell.Entry.Text) ? null :
                                IdSsnCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            PassportNumber = string.IsNullOrWhiteSpace(IdPassportNumberCell.Entry.Text) ? null :
                                IdPassportNumberCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            LicenseNumber = string.IsNullOrWhiteSpace(IdLicenseNumberCell.Entry.Text) ? null :
                                IdLicenseNumberCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Email = string.IsNullOrWhiteSpace(IdEmailCell.Entry.Text) ? null :
                                IdEmailCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Phone = string.IsNullOrWhiteSpace(IdPhoneCell.Entry.Text) ? null :
                                IdPhoneCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Address1 = string.IsNullOrWhiteSpace(IdAddress1Cell.Entry.Text) ? null :
                                IdAddress1Cell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Address2 = string.IsNullOrWhiteSpace(IdAddress2Cell.Entry.Text) ? null :
                                IdAddress2Cell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Address3 = string.IsNullOrWhiteSpace(IdAddress3Cell.Entry.Text) ? null :
                                IdAddress3Cell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            City = string.IsNullOrWhiteSpace(IdCityCell.Entry.Text) ? null :
                                IdCityCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            State = string.IsNullOrWhiteSpace(IdStateCell.Entry.Text) ? null :
                                IdStateCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            PostalCode = string.IsNullOrWhiteSpace(IdPostalCodeCell.Entry.Text) ? null :
                                IdPostalCodeCell.Entry.Text.Encrypt(Cipher.OrganizationId),
                            Country = string.IsNullOrWhiteSpace(IdCountryCell.Entry.Text) ? null :
                                IdCountryCell.Entry.Text.Encrypt(Cipher.OrganizationId)
                        };
                        break;
                    default:
                        break;
                }

                if(FolderCell.Picker.SelectedIndex > 0)
                {
                    Cipher.FolderId = Folders.ElementAt(FolderCell.Picker.SelectedIndex - 1).Id;
                }
                else
                {
                    Cipher.FolderId = null;
                }

                _userDialogs.ShowLoading(AppResources.Saving, MaskType.Black);
                var saveTask = await _cipherService.SaveAsync(Cipher);
                _userDialogs.HideLoading();

                if(saveTask.Succeeded)
                {
                    _deviceActionService.Toast(AppResources.ItemUpdated);
                    _googleAnalyticsService.TrackAppEvent("EditedCipher");
                    await Navigation.PopForDeviceAsync();
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

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
            }

            NameCell?.InitEvents();
            NotesCell?.InitEvents();
            FolderCell?.InitEvents();

            if(AttachmentsCell != null)
            {
                AttachmentsCell.Tapped += AttachmentsCell_Tapped;
            }
            if(CustomFieldsCell != null)
            {
                CustomFieldsCell.Tapped += CustomFieldsCell_Tapped;
            }
            if(DeleteCell != null)
            {
                DeleteCell.Tapped += DeleteCell_Tapped;
            }

            switch(Cipher.Type)
            {
                case CipherType.Login:
                    LoginPasswordCell?.InitEvents();
                    LoginUsernameCell?.InitEvents();
                    LoginUriCell?.InitEvents();
                    LoginTotpCell?.InitEvents();
                    if(LoginPasswordCell?.Button != null)
                    {
                        LoginPasswordCell.Button.Clicked += PasswordButton_Clicked;
                    }
                    if(LoginGenerateCell != null)
                    {
                        LoginGenerateCell.Tapped += GenerateCell_Tapped;
                    }
                    if(LoginTotpCell?.Button != null)
                    {
                        LoginTotpCell.Button.Clicked += TotpButton_Clicked;
                    }
                    break;
                case CipherType.Card:
                    CardBrandCell?.InitEvents();
                    CardCodeCell?.InitEvents();
                    CardExpMonthCell?.InitEvents();
                    CardExpYearCell?.InitEvents();
                    CardNameCell?.InitEvents();
                    CardNumberCell?.InitEvents();
                    break;
                case CipherType.Identity:
                    IdTitleCell?.InitEvents();
                    IdFirstNameCell?.InitEvents();
                    IdMiddleNameCell?.InitEvents();
                    IdLastNameCell?.InitEvents();
                    IdUsernameCell?.InitEvents();
                    IdCompanyCell?.InitEvents();
                    IdSsnCell?.InitEvents();
                    IdPassportNumberCell?.InitEvents();
                    IdLicenseNumberCell?.InitEvents();
                    IdEmailCell?.InitEvents();
                    IdPhoneCell?.InitEvents();
                    IdAddress1Cell?.InitEvents();
                    IdAddress2Cell?.InitEvents();
                    IdAddress3Cell?.InitEvents();
                    IdCityCell?.InitEvents();
                    IdStateCell?.InitEvents();
                    IdPostalCodeCell?.InitEvents();
                    IdCountryCell?.InitEvents();
                    break;
                default:
                    break;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            NameCell?.Dispose();
            NotesCell?.Dispose();
            FolderCell?.Dispose();

            if(AttachmentsCell != null)
            {
                AttachmentsCell.Tapped -= AttachmentsCell_Tapped;
            }
            if(CustomFieldsCell != null)
            {
                CustomFieldsCell.Tapped -= CustomFieldsCell_Tapped;
            }
            if(DeleteCell != null)
            {
                DeleteCell.Tapped -= DeleteCell_Tapped;
            }

            switch(Cipher.Type)
            {
                case CipherType.Login:
                    LoginTotpCell?.Dispose();
                    LoginPasswordCell?.Dispose();
                    LoginUsernameCell?.Dispose();
                    LoginUriCell?.Dispose();
                    if(LoginPasswordCell?.Button != null)
                    {
                        LoginPasswordCell.Button.Clicked -= PasswordButton_Clicked;
                    }
                    if(LoginGenerateCell != null)
                    {
                        LoginGenerateCell.Tapped -= GenerateCell_Tapped;
                    }
                    if(LoginTotpCell?.Button != null)
                    {
                        LoginTotpCell.Button.Clicked -= TotpButton_Clicked;
                    }
                    break;
                case CipherType.Card:
                    CardBrandCell?.Dispose();
                    CardCodeCell?.Dispose();
                    CardExpMonthCell?.Dispose();
                    CardExpYearCell?.Dispose();
                    CardNameCell?.Dispose();
                    CardNumberCell?.Dispose();
                    break;
                case CipherType.Identity:
                    IdTitleCell?.Dispose();
                    IdFirstNameCell?.Dispose();
                    IdMiddleNameCell?.Dispose();
                    IdLastNameCell?.Dispose();
                    IdUsernameCell?.Dispose();
                    IdCompanyCell?.Dispose();
                    IdSsnCell?.Dispose();
                    IdPassportNumberCell?.Dispose();
                    IdLicenseNumberCell?.Dispose();
                    IdEmailCell?.Dispose();
                    IdPhoneCell?.Dispose();
                    IdAddress1Cell?.Dispose();
                    IdAddress2Cell?.Dispose();
                    IdAddress3Cell?.Dispose();
                    IdCityCell?.Dispose();
                    IdStateCell?.Dispose();
                    IdPostalCodeCell?.Dispose();
                    IdCountryCell?.Dispose();
                    break;
                default:
                    break;
            }
        }

        private void PasswordButton_Clicked(object sender, EventArgs e)
        {
            LoginPasswordCell.Entry.InvokeToggleIsPassword();
            LoginPasswordCell.Button.Image =
                "eye" + (!LoginPasswordCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty) + ".png";
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

        private async void GenerateCell_Tapped(object sender, EventArgs e)
        {
            if(!string.IsNullOrWhiteSpace(LoginPasswordCell.Entry.Text)
                && !(await DisplayAlert(null, AppResources.PasswordOverrideAlert, AppResources.Yes, AppResources.No)))
            {
                return;
            }

            var page = new ToolsPasswordGeneratorPage((password) =>
            {
                LoginPasswordCell.Entry.Text = password;
                _deviceActionService.Toast(AppResources.PasswordGenerated);
            });
            await Navigation.PushForDeviceAsync(page);
        }

        private async void AttachmentsCell_Tapped(object sender, EventArgs e)
        {
            var page = new ExtendedNavigationPage(new VaultAttachmentsPage(_cipherId));
            await Navigation.PushModalAsync(page);
        }

        private async void CustomFieldsCell_Tapped(object sender, EventArgs e)
        {
            var page = new ExtendedNavigationPage(new VaultCustomFieldsPage(_cipherId));
            await Navigation.PushModalAsync(page);
        }

        private async void DeleteCell_Tapped(object sender, EventArgs e)
        {
            if(!_connectivity.IsConnected)
            {
                AlertNoConnection();
                return;
            }

            var confirmed = await DisplayAlert(null, AppResources.DoYouReallyWantToDelete, AppResources.Yes,
                AppResources.No);
            if(!confirmed)
            {
                return;
            }

            _userDialogs.ShowLoading(AppResources.Deleting, MaskType.Black);
            var deleteTask = await _cipherService.DeleteAsync(_cipherId);
            _userDialogs.HideLoading();

            if(deleteTask.Succeeded)
            {
                _deviceActionService.Toast(AppResources.ItemDeleted);
                _googleAnalyticsService.TrackAppEvent("DeletedCipher");
                await Navigation.PopForDeviceAsync();
            }
            else if(deleteTask.Errors.Count() > 0)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, deleteTask.Errors.First().Message, AppResources.Ok);
            }
            else
            {
                await DisplayAlert(null, AppResources.AnErrorHasOccurred, AppResources.Ok);
            }
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }
    }
}
