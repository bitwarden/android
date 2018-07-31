using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using System.Threading.Tasks;
using Bit.App.Utilities;
using System.Collections.Generic;
using Bit.App.Models;
using System.Linq;
using Bit.App.Enums;

namespace Bit.App.Pages
{
    public class VaultViewCipherPage : ExtendedContentPage
    {
        private readonly CipherType _type;
        private readonly string _cipherId;
        private readonly ICipherService _cipherService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ITokenService _tokenService;
        private DateTime? _timerStarted = null;
        private TimeSpan _timerMaxLength = TimeSpan.FromMinutes(5);

        public VaultViewCipherPage(CipherType type, string cipherId)
        {
            _type = type;
            _cipherId = cipherId;
            _cipherService = Resolver.Resolve<ICipherService>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _tokenService = Resolver.Resolve<ITokenService>();

            Init();
        }

        public Fab Fab { get; set; }
        private VaultViewCipherPageModel Model { get; set; } = new VaultViewCipherPageModel();
        private ExtendedTableView Table { get; set; }
        private TableSection ItemInformationSection { get; set; }
        public TableSection UrisSection { get; set; }
        private TableSection NotesSection { get; set; }
        private TableSection AttachmentsSection { get; set; }
        private TableSection FieldsSection { get; set; }
        public TableSection OtherSection { get; set; }
        public LabeledValueCell NotesCell { get; set; }
        private EditCipherToolBarItem EditItem { get; set; }
        public List<LabeledValueCell> FieldsCells { get; set; }
        public List<AttachmentViewCell> AttachmentCells { get; set; }

        // Login
        public LabeledValueCell LoginUsernameCell { get; set; }
        public LabeledValueCell LoginPasswordCell { get; set; }
        public LabeledValueCell LoginPasswordRevisionDateCell { get; set; }
        public LabeledValueCell LoginTotpCodeCell { get; set; }

        // Card
        public LabeledValueCell CardNameCell { get; set; }
        public LabeledValueCell CardNumberCell { get; set; }
        public LabeledValueCell CardBrandCell { get; set; }
        public LabeledValueCell CardExpCell { get; set; }
        public LabeledValueCell CardCodeCell { get; set; }

        // Card
        public LabeledValueCell IdNameCell { get; set; }
        public LabeledValueCell IdUsernameCell { get; set; }
        public LabeledValueCell IdCompanyCell { get; set; }
        public LabeledValueCell IdSsnCell { get; set; }
        public LabeledValueCell IdPassportNumberCell { get; set; }
        public LabeledValueCell IdLicenseNumberCell { get; set; }
        public LabeledValueCell IdEmailCell { get; set; }
        public LabeledValueCell IdPhoneCell { get; set; }
        public LabeledValueCell IdAddressCell { get; set; }

        private void Init()
        {
            if(Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this));
            }

            InitProps();

            var fabLayout = new FabLayout(Table);
            if(Device.RuntimePlatform == Device.Android)
            {
                Fab = new Fab(fabLayout, "pencil.png", async (sender, args) =>
                {
                    await Navigation.PushForDeviceAsync(new VaultEditCipherPage(_cipherId));
                });
            }
            else
            {
                EditItem = new EditCipherToolBarItem(this, _cipherId);
                ToolbarItems.Add(EditItem);
            }

            Content = fabLayout;
            Title = AppResources.ViewItem;
            BindingContext = Model;
        }

        public void InitProps()
        {
            // Name
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.Name));
            nameCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            // Notes
            NotesCell = new LabeledValueCell();
            NotesCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.Notes));
            NotesCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            var revisionDateCell = new LabeledValueCell(AppResources.DateUpdated);
            revisionDateCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.RevisionDate));
            revisionDateCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            switch(_type)
            {
                case CipherType.Login:
                    // Username
                    LoginUsernameCell = new LabeledValueCell(AppResources.Username, button1Image: "clipboard.png");
                    LoginUsernameCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.LoginUsername));
                    LoginUsernameCell.Button1.Command =
                        new Command(() => Copy(Model.LoginUsername, AppResources.Username));
                    LoginUsernameCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    // Password
                    LoginPasswordCell = new LabeledValueCell(AppResources.Password, button1Image: string.Empty,
                        button2Image: "clipboard.png");
                    LoginPasswordCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.MaskedLoginPassword));
                    LoginPasswordCell.Button1.SetBinding(Button.ImageProperty,
                        nameof(VaultViewCipherPageModel.LoginShowHideImage));
                    LoginPasswordCell.Button1.Command =
                        new Command(() => Model.RevealLoginPassword = !Model.RevealLoginPassword);
                    LoginPasswordCell.Button2.Command =
                        new Command(() => Copy(Model.LoginPassword, AppResources.Password));
                    LoginPasswordCell.Value.FontFamily =
                        Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");
                    LoginPasswordCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    // Totp
                    LoginTotpCodeCell = new LabeledValueCell(
                        AppResources.VerificationCodeTotp, button1Image: "clipboard.png", subText: "--");
                    LoginTotpCodeCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.LoginTotpCodeFormatted));
                    LoginTotpCodeCell.Value.SetBinding(Label.TextColorProperty,
                        nameof(VaultViewCipherPageModel.LoginTotpColor));
                    LoginTotpCodeCell.Button1.Command =
                        new Command(() => Copy(Model.LoginTotpCode, AppResources.VerificationCodeTotp));
                    LoginTotpCodeCell.Sub.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.LoginTotpSecond));
                    LoginTotpCodeCell.Sub.SetBinding(Label.TextColorProperty,
                        nameof(VaultViewCipherPageModel.LoginTotpColor));
                    LoginTotpCodeCell.Value.FontFamily =
                        Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");

                    // Password Revision Date
                    LoginPasswordRevisionDateCell = new LabeledValueCell(AppResources.DatePasswordUpdated);
                    LoginPasswordRevisionDateCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.PasswordRevisionDate));
                    LoginPasswordRevisionDateCell.Value.LineBreakMode = LineBreakMode.WordWrap;
                    break;
                case CipherType.Card:
                    CardNameCell = new LabeledValueCell(AppResources.CardholderName);
                    CardNameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.CardName));

                    CardNumberCell = new LabeledValueCell(AppResources.Number, button1Image: "clipboard.png");
                    CardNumberCell.Button1.Command = new Command(() => Copy(Model.CardNumber, AppResources.Number));
                    CardNumberCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.CardNumber));
                    CardNumberCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    CardBrandCell = new LabeledValueCell(AppResources.Brand);
                    CardBrandCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.CardBrand));

                    CardExpCell = new LabeledValueCell(AppResources.Expiration);
                    CardExpCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.CardExp));

                    CardCodeCell = new LabeledValueCell(AppResources.SecurityCode, button1Image: string.Empty,
                        button2Image: "clipboard.png");
                    CardCodeCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.MaskedCardCode));
                    CardCodeCell.Button1.SetBinding(Button.ImageProperty,
                        nameof(VaultViewCipherPageModel.CardCodeShowHideImage));
                    CardCodeCell.Button1.Command = new Command(() => Model.RevealCardCode = !Model.RevealCardCode);
                    CardCodeCell.Button2.Command = new Command(() => Copy(Model.CardCode, AppResources.SecurityCode));
                    CardCodeCell.Value.FontFamily =
                        Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");
                    break;
                case CipherType.Identity:
                    IdNameCell = new LabeledValueCell(AppResources.Name);
                    IdNameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdName));
                    IdNameCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    IdUsernameCell = new LabeledValueCell(AppResources.Username, button1Image: "clipboard.png");
                    IdUsernameCell.Button1.Command = new Command(() => Copy(Model.IdUsername, AppResources.Username));
                    IdUsernameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdUsername));
                    IdUsernameCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    IdCompanyCell = new LabeledValueCell(AppResources.Company);
                    IdCompanyCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdCompany));

                    IdSsnCell = new LabeledValueCell(AppResources.SSN);
                    IdSsnCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdSsn));

                    IdPassportNumberCell = new LabeledValueCell(AppResources.PassportNumber,
                        button1Image: "clipboard.png");
                    IdPassportNumberCell.Button1.Command =
                        new Command(() => Copy(Model.IdPassportNumber, AppResources.PassportNumber));
                    IdPassportNumberCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.IdPassportNumber));
                    IdPassportNumberCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    IdLicenseNumberCell = new LabeledValueCell(AppResources.LicenseNumber,
                        button1Image: "clipboard.png");
                    IdLicenseNumberCell.Button1.Command =
                        new Command(() => Copy(Model.IdLicenseNumber, AppResources.LicenseNumber));
                    IdLicenseNumberCell.Value.SetBinding(Label.TextProperty,
                        nameof(VaultViewCipherPageModel.IdLicenseNumber));
                    IdLicenseNumberCell.Value.LineBreakMode = LineBreakMode.WordWrap;

                    IdEmailCell = new LabeledValueCell(AppResources.Email);
                    IdEmailCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdEmail));

                    IdPhoneCell = new LabeledValueCell(AppResources.Phone);
                    IdPhoneCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdPhone));

                    IdAddressCell = new LabeledValueCell(AppResources.Address, button1Image: "clipboard.png");
                    IdAddressCell.Button1.Command = new Command(() => Copy(Model.IdAddress, AppResources.Address));
                    IdAddressCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewCipherPageModel.IdAddress));
                    IdAddressCell.Value.LineBreakMode = LineBreakMode.WordWrap;
                    break;
                default:
                    break;
            }

            ItemInformationSection = new TableSection(AppResources.ItemInformation)
            {
                nameCell
            };

            NotesSection = new TableSection(AppResources.Notes)
            {
                NotesCell
            };

            OtherSection = new TableSection(Helpers.GetEmptyTableSectionTitle())
            {
                revisionDateCell
            };

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                EnableSelection = true,
                Root = new TableRoot
                {
                    ItemInformationSection
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 70;
            }
            else if(Device.RuntimePlatform == Device.Android)
            {
                Table.BottomPadding = 170;
            }
        }

        protected async override void OnAppearing()
        {
            NotesCell.Tapped += NotesCell_Tapped;
            EditItem?.InitEvents();

            var cipher = await _cipherService.GetByIdAsync(_cipherId);
            if(cipher == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            Model.Update(cipher);
            BuildTable(cipher);
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            _timerStarted = null;
            NotesCell.Tapped -= NotesCell_Tapped;
            EditItem?.Dispose();
            CleanupAttachmentCells();
        }

        private void BuildTable(Cipher cipher)
        {
            // URIs
            if(UrisSection != null && Table.Root.Contains(UrisSection))
            {
                Table.Root.Remove(UrisSection);
            }
            if(Model.ShowLoginUris)
            {
                UrisSection = new TableSection(Helpers.GetEmptyTableSectionTitle());
                foreach(var uri in Model.LoginUris)
                {
                    UrisSection.Add(new UriViewCell(this, uri));
                }
                Table.Root.Add(UrisSection);
            }

            // Notes
            if(Table.Root.Contains(NotesSection))
            {
                Table.Root.Remove(NotesSection);
            }
            if(Model.ShowNotes)
            {
                Table.Root.Add(NotesSection);
            }

            // Fields
            if(Table.Root.Contains(FieldsSection))
            {
                Table.Root.Remove(FieldsSection);
            }
            if(Model.ShowFields)
            {
                FieldsSection = new TableSection(AppResources.CustomFields);
                foreach(var field in Model.Fields)
                {
                    FieldViewCell fieldCell;
                    switch(field.Type)
                    {
                        case FieldType.Text:
                            fieldCell = new FieldViewCell(this, field, null);
                            break;
                        case FieldType.Hidden:
                            fieldCell = new FieldViewCell(this, field, null, null);
                            break;
                        case FieldType.Boolean:
                            fieldCell = new FieldViewCell(this, field);
                            break;
                        default:
                            continue;
                    }
                    FieldsSection.Add(fieldCell);
                }
                Table.Root.Add(FieldsSection);
            }

            // Attachments
            CleanupAttachmentCells();
            if(Table.Root.Contains(AttachmentsSection))
            {
                Table.Root.Remove(AttachmentsSection);
            }
            if(Model.ShowAttachments && (_tokenService.TokenPremium || cipher.OrganizationId != null))
            {
                AttachmentsSection = new TableSection(AppResources.Attachments);
                AttachmentCells = new List<AttachmentViewCell>();
                foreach(var attachment in Model.Attachments.OrderBy(s => s.Name))
                {
                    var attachmentCell = new AttachmentViewCell(attachment, async () =>
                    {
                        await OpenAttachmentAsync(cipher, attachment);
                    });
                    AttachmentCells.Add(attachmentCell);
                    AttachmentsSection.Add(attachmentCell);
                    attachmentCell.InitEvents();
                }
                Table.Root.Add(AttachmentsSection);
            }

            // Other
            if(Table.Root.Contains(OtherSection))
            {
                Table.Root.Remove(OtherSection);
            }
            Table.Root.Add(OtherSection);

            // Various types
            switch(cipher.Type)
            {
                case CipherType.Login:
                    if(OtherSection.Contains(LoginPasswordRevisionDateCell))
                    {
                        OtherSection.Remove(LoginPasswordRevisionDateCell);
                    }
                    if(Model.ShowPasswordRevisionDate)
                    {
                        OtherSection.Add(LoginPasswordRevisionDateCell);
                    }

                    AddSectionCell(LoginUsernameCell, Model.ShowLoginUsername);
                    AddSectionCell(LoginPasswordCell, Model.ShowLoginPassword);

                    if(ItemInformationSection.Contains(LoginTotpCodeCell))
                    {
                        ItemInformationSection.Remove(LoginTotpCodeCell);
                    }
                    if(cipher.Login?.Totp != null && (_tokenService.TokenPremium || cipher.OrganizationUseTotp))
                    {
                        var totpKey = cipher.Login?.Totp.Decrypt(cipher.OrganizationId);
                        if(!string.IsNullOrWhiteSpace(totpKey))
                        {
                            var otpParams = new OtpAuth(totpKey);
                            Model.LoginTotpCode = Crypto.Totp(totpKey);
                            if(!string.IsNullOrWhiteSpace(Model.LoginTotpCode))
                            {
                                TotpTick(totpKey, otpParams.Period);
                                _timerStarted = DateTime.Now;
                                Device.StartTimer(new TimeSpan(0, 0, 1), () =>
                                {
                                    if(_timerStarted == null || (DateTime.Now - _timerStarted) > _timerMaxLength)
                                    {
                                        return false;
                                    }

                                    TotpTick(totpKey, otpParams.Period);
                                    return true;
                                });

                                ItemInformationSection.Add(LoginTotpCodeCell);
                            }
                        }
                    }
                    break;
                case CipherType.Card:
                    AddSectionCell(CardNameCell, Model.ShowCardName);
                    AddSectionCell(CardNumberCell, Model.ShowCardNumber);
                    AddSectionCell(CardBrandCell, Model.ShowCardBrand);
                    AddSectionCell(CardExpCell, Model.ShowCardExp);
                    AddSectionCell(CardCodeCell, Model.ShowCardCode);
                    break;
                case CipherType.Identity:
                    AddSectionCell(IdNameCell, Model.ShowIdName);
                    AddSectionCell(IdUsernameCell, Model.ShowIdUsername);
                    AddSectionCell(IdCompanyCell, Model.ShowIdCompany);
                    AddSectionCell(IdSsnCell, Model.ShowIdSsn);
                    AddSectionCell(IdPassportNumberCell, Model.ShowIdPassportNumber);
                    AddSectionCell(IdLicenseNumberCell, Model.ShowIdLicenseNumber);
                    AddSectionCell(IdEmailCell, Model.ShowIdEmail);
                    AddSectionCell(IdPhoneCell, Model.ShowIdPhone);
                    AddSectionCell(IdAddressCell, Model.ShowIdAddress);
                    break;
                default:
                    break;
            }
        }

        private void AddSectionCell(LabeledValueCell cell, bool show)
        {
            if(ItemInformationSection.Contains(cell))
            {
                ItemInformationSection.Remove(cell);
            }
            if(show)
            {
                ItemInformationSection.Add(cell);
            }
        }

        private void CleanupAttachmentCells()
        {
            if(AttachmentCells != null)
            {
                foreach(var cell in AttachmentCells)
                {
                    cell.Dispose();
                }
            }
        }

        private async Task OpenAttachmentAsync(Cipher cipher, VaultViewCipherPageModel.Attachment attachment)
        {
            if(!_tokenService.TokenPremium && !cipher.OrganizationUseTotp)
            {
                await DisplayAlert(null, AppResources.PremiumRequired, AppResources.Ok);
                return;
            }

            // 10 MB warning
            if(attachment.Size >= 10485760 && !(await DisplayAlert(
                    null, string.Format(AppResources.AttachmentLargeWarning, attachment.SizeName),
                    AppResources.Yes, AppResources.No)))
            {
                return;
            }

            if(!_deviceActionService.CanOpenFile(attachment.Name))
            {
                await DisplayAlert(null, AppResources.UnableToOpenFile, AppResources.Ok);
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.Downloading);
            var data = await _cipherService.DownloadAndDecryptAttachmentAsync(attachment.Url, cipher.OrganizationId);
            await _deviceActionService.HideLoadingAsync();

            if(data == null)
            {
                await DisplayAlert(null, AppResources.UnableToDownloadFile, AppResources.Ok);
                return;
            }

            if(!_deviceActionService.OpenFile(data, attachment.Id, attachment.Name))
            {
                await DisplayAlert(null, AppResources.UnableToOpenFile, AppResources.Ok);
                return;
            }
        }

        private void NotesCell_Tapped(object sender, EventArgs e)
        {
            Copy(Model.Notes, AppResources.Notes);
        }

        private void Copy(string copyText, string alertLabel)
        {
            _deviceActionService.CopyToClipboard(copyText);
            _deviceActionService.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private void TotpTick(string totpKey, int interval)
        {
            var now = Helpers.EpocUtcNow() / 1000;
            var mod = now % interval;
            Model.LoginTotpSecond = (int)(interval - mod);

            if(mod == 0)
            {
                Model.LoginTotpCode = Crypto.Totp(totpKey);
            }
        }

        private class EditCipherToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultViewCipherPage _page;
            private readonly string _cipherId;

            public EditCipherToolBarItem(VaultViewCipherPage page, string cipherId)
            {
                _page = page;
                _cipherId = cipherId;
                Text = AppResources.Edit;
                Icon = Helpers.ToolbarImage("cog.png");
                ClickAction = async () => await ClickedItem();
            }

            private async Task ClickedItem()
            {
                var page = new VaultEditCipherPage(_cipherId);
                await _page.Navigation.PushForDeviceAsync(page);
            }
        }

        public class AttachmentViewCell : LabeledRightDetailCell, IDisposable
        {
            private readonly Action _tapped;

            public AttachmentViewCell(VaultViewCipherPageModel.Attachment attachment, Action tappedAction)
            {
                _tapped = tappedAction;
                Label.Text = attachment.Name;
                Detail.Text = attachment.SizeName;
                Icon.Source = "download.png";
                BackgroundColor = Color.White;
                Detail.MinimumWidthRequest = 100;
            }

            public void InitEvents()
            {
                Tapped += AttachmentViewCell_Tapped;
            }

            public void Dispose()
            {
                Tapped -= AttachmentViewCell_Tapped;
            }

            private void AttachmentViewCell_Tapped(object sender, EventArgs e)
            {
                _tapped?.Invoke();
            }
        }

        public class FieldViewCell : LabeledValueCell
        {
            public FieldViewCell(VaultViewCipherPage page, VaultViewCipherPageModel.Field field)
                : base(field.Name, field.Value == "true" ? "✓" : "-")
            {
                Init(page, field, null);
            }

            public FieldViewCell(VaultViewCipherPage page, VaultViewCipherPageModel.Field field, bool? a)
                : base(field.Name, field.Value, "clipboard.png")
            {
                Init(page, field, Button1);
            }

            public FieldViewCell(VaultViewCipherPage page, VaultViewCipherPageModel.Field field, bool? a, bool? b)
                : base(field.Name, field.MaskedValue, string.Empty, "clipboard.png")
            {
                Value.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");
                Button1.Image = "eye";
                Button1.Command = new Command(() =>
                {
                    field.Revealed = !field.Revealed;
                    if(field.Revealed)
                    {
                        Button1.Image = "eye_slash.png";
                        Value.Text = field.Value;
                    }
                    else
                    {
                        Button1.Image = "eye.png";
                        Value.Text = field.MaskedValue;
                    }
                });

                Init(page, field, Button2);
            }

            private void Init(VaultViewCipherPage page, VaultViewCipherPageModel.Field field, ExtendedButton copyButton)
            {
                Value.LineBreakMode = LineBreakMode.WordWrap;
                if(copyButton != null)
                {
                    copyButton.Command = new Command(() => page.Copy(field.Value, field.Name));
                }
            }
        }

        public class UriViewCell : LabeledValueCell
        {
            public UriViewCell(VaultViewCipherPage page, VaultViewCipherPageModel.LoginUri uri)
                : base(uri.Label, uri.Host, uri.ShowLaunch ? "launch.png" : null, "clipboard.png")
            {
                Value.LineBreakMode = LineBreakMode.TailTruncation;
                if(Button1 != null)
                {
                    Button1.Command = new Command(async () =>
                    {
                        if(Device.RuntimePlatform == Device.Android && uri.IsApp)
                        {
                            await page._deviceActionService.LaunchAppAsync(uri.Value, page);
                        }
                        else if(uri.IsWebsite)
                        {
                            Device.OpenUri(new Uri(uri.Value));
                        }
                    });
                }
                Button2.Command = new Command(() => page.Copy(uri.Value, AppResources.URI));
            }
        }
    }
}
