using System;
using Acr.UserDialogs;
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
        private readonly string _loginId;
        private readonly ICipherService _cipherService;
        private readonly IUserDialogs _userDialogs;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ITokenService _tokenService;
        private bool _pageDisappeared = true;

        public VaultViewCipherPage(string loginId)
        {
            _loginId = loginId;
            _cipherService = Resolver.Resolve<ICipherService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _tokenService = Resolver.Resolve<ITokenService>();

            Init();
        }

        private VaultViewLoginPageModel Model { get; set; } = new VaultViewLoginPageModel();
        private ExtendedTableView Table { get; set; }
        private TableSection LoginInformationSection { get; set; }
        private TableSection NotesSection { get; set; }
        private TableSection AttachmentsSection { get; set; }
        private TableSection FieldsSection { get; set; }
        public LabeledValueCell UsernameCell { get; set; }
        public LabeledValueCell PasswordCell { get; set; }
        public LabeledValueCell UriCell { get; set; }
        public LabeledValueCell NotesCell { get; set; }
        public LabeledValueCell TotpCodeCell { get; set; }
        private EditLoginToolBarItem EditItem { get; set; }
        public List<LabeledValueCell> FieldsCells { get; set; }
        public List<AttachmentViewCell> AttachmentCells { get; set; }

        private void Init()
        {
            EditItem = new EditLoginToolBarItem(this, _loginId);
            ToolbarItems.Add(EditItem);
            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Windows)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this));
            }

            // Name
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Name));

            // Username
            UsernameCell = new LabeledValueCell(AppResources.Username, button1Image: "clipboard.png");
            UsernameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Username));
            UsernameCell.Button1.Command = new Command(() => Copy(Model.Username, AppResources.Username));
            UsernameCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            // Password
            PasswordCell = new LabeledValueCell(AppResources.Password, button1Image: string.Empty,
                button2Image: "clipboard.png");
            PasswordCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.MaskedPassword));
            PasswordCell.Button1.SetBinding(Button.ImageProperty, nameof(VaultViewLoginPageModel.ShowHideImage));
            if(Device.RuntimePlatform == Device.iOS)
            {
                PasswordCell.Button1.Margin = new Thickness(10, 0);
            }
            PasswordCell.Button1.Command = new Command(() => Model.RevealPassword = !Model.RevealPassword);
            PasswordCell.Button2.Command = new Command(() => Copy(Model.Password, AppResources.Password));
            PasswordCell.Value.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier");
            PasswordCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            // URI
            UriCell = new LabeledValueCell(AppResources.Website, button1Image: "launch.png");
            UriCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.UriHost));
            UriCell.Button1.SetBinding(IsVisibleProperty, nameof(VaultViewLoginPageModel.ShowLaunch));
            UriCell.Button1.Command = new Command(() =>
            {
                if(Device.RuntimePlatform == Device.Android && Model.Uri.StartsWith("androidapp://"))
                {
                    MessagingCenter.Send(Application.Current, "LaunchApp", Model.Uri);
                }
                else if(Model.Uri.StartsWith("http://") || Model.Uri.StartsWith("https://"))
                {
                    Device.OpenUri(new Uri(Model.Uri));
                }
            });

            // Totp
            TotpCodeCell = new LabeledValueCell(AppResources.VerificationCodeTotp, button1Image: "clipboard.png", subText: "--");
            TotpCodeCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.TotpCodeFormatted));
            TotpCodeCell.Value.SetBinding(Label.TextColorProperty, nameof(VaultViewLoginPageModel.TotpColor));
            TotpCodeCell.Button1.Command = new Command(() => Copy(Model.TotpCode, AppResources.VerificationCodeTotp));
            TotpCodeCell.Sub.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.TotpSecond));
            TotpCodeCell.Sub.SetBinding(Label.TextColorProperty, nameof(VaultViewLoginPageModel.TotpColor));
            TotpCodeCell.Value.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier");

            // Notes
            NotesCell = new LabeledValueCell();
            NotesCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Notes));
            NotesCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            LoginInformationSection = new TableSection(AppResources.ItemInformation)
            {
                nameCell
            };

            NotesSection = new TableSection(AppResources.Notes)
            {
                NotesCell
            };

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                EnableSelection = true,
                Root = new TableRoot
                {
                    LoginInformationSection
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 70;
            }

            Title = AppResources.ViewItem;
            Content = Table;
            BindingContext = Model;
        }

        protected async override void OnAppearing()
        {
            _pageDisappeared = false;
            NotesCell.Tapped += NotesCell_Tapped;
            EditItem.InitEvents();

            var cipher = await _cipherService.GetByIdAsync(_loginId);
            if(cipher == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            Model.Update(cipher);

            if(LoginInformationSection.Contains(UriCell))
            {
                LoginInformationSection.Remove(UriCell);
            }
            if(Model.ShowUri)
            {
                LoginInformationSection.Add(UriCell);
            }

            if(LoginInformationSection.Contains(UsernameCell))
            {
                LoginInformationSection.Remove(UsernameCell);
            }
            if(Model.ShowUsername)
            {
                LoginInformationSection.Add(UsernameCell);
            }

            if(LoginInformationSection.Contains(PasswordCell))
            {
                LoginInformationSection.Remove(PasswordCell);
            }
            if(Model.ShowPassword)
            {
                LoginInformationSection.Add(PasswordCell);
            }

            if(Table.Root.Contains(NotesSection))
            {
                Table.Root.Remove(NotesSection);
            }
            if(Model.ShowNotes)
            {
                Table.Root.Add(NotesSection);
            }

            // Totp
            if(LoginInformationSection.Contains(TotpCodeCell))
            {
                LoginInformationSection.Remove(TotpCodeCell);
            }
            if(cipher.Login?.Totp != null && (_tokenService.TokenPremium || cipher.OrganizationUseTotp))
            {
                var totpKey = cipher.Login?.Totp.Decrypt(cipher.OrganizationId);
                if(!string.IsNullOrWhiteSpace(totpKey))
                {
                    Model.TotpCode = Crypto.Totp(totpKey);
                    if(!string.IsNullOrWhiteSpace(Model.TotpCode))
                    {
                        TotpTick(totpKey);
                        Device.StartTimer(new TimeSpan(0, 0, 1), () =>
                        {
                            if(_pageDisappeared)
                            {
                                return false;
                            }

                            TotpTick(totpKey);
                            return true;
                        });

                        LoginInformationSection.Add(TotpCodeCell);
                    }
                }
            }

            CleanupAttachmentCells();
            if(Table.Root.Contains(AttachmentsSection))
            {
                Table.Root.Remove(AttachmentsSection);
            }
            if(Model.ShowAttachments && _tokenService.TokenPremium)
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

            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            _pageDisappeared = true;
            NotesCell.Tapped -= NotesCell_Tapped;
            EditItem.Dispose();
            CleanupAttachmentCells();
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

        private async Task OpenAttachmentAsync(Cipher login, VaultViewLoginPageModel.Attachment attachment)
        {
            if(!_tokenService.TokenPremium && !login.OrganizationUseTotp)
            {
                _userDialogs.Alert(AppResources.PremiumRequired);
                return;
            }

            // 10 MB warning
            if(attachment.Size >= 10485760 && !(await _userDialogs.ConfirmAsync(
                    string.Format(AppResources.AttachmentLargeWarning, attachment.SizeName), null,
                    AppResources.Yes, AppResources.No)))
            {
                return;
            }

            if(!_deviceActionService.CanOpenFile(attachment.Name))
            {
                await _userDialogs.AlertAsync(AppResources.UnableToOpenFile, null, AppResources.Ok);
                return;
            }

            _userDialogs.ShowLoading(AppResources.Downloading, MaskType.Black);
            var data = await _cipherService.DownloadAndDecryptAttachmentAsync(attachment.Url, login.OrganizationId);
            _userDialogs.HideLoading();
            if(data == null)
            {
                await _userDialogs.AlertAsync(AppResources.UnableToDownloadFile, null, AppResources.Ok);
                return;
            }

            if(!_deviceActionService.OpenFile(data, attachment.Id, attachment.Name))
            {
                await _userDialogs.AlertAsync(AppResources.UnableToOpenFile, null, AppResources.Ok);
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
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private void TotpTick(string totpKey)
        {
            var now = Helpers.EpocUtcNow() / 1000;
            var mod = now % 30;
            Model.TotpSecond = (int)(30 - mod);

            if(mod == 0)
            {
                Model.TotpCode = Crypto.Totp(totpKey);
            }
        }

        private class EditLoginToolBarItem : ExtendedToolbarItem
        {
            private readonly VaultViewCipherPage _page;
            private readonly string _loginId;

            public EditLoginToolBarItem(VaultViewCipherPage page, string loginId)
            {
                _page = page;
                _loginId = loginId;
                Text = AppResources.Edit;
                ClickAction = async () => await ClickedItem();
            }

            private async Task ClickedItem()
            {
                var page = new VaultEditCipherPage(_loginId);
                await _page.Navigation.PushForDeviceAsync(page);
            }
        }

        public class AttachmentViewCell : LabeledRightDetailCell, IDisposable
        {
            private readonly Action _tapped;

            public AttachmentViewCell(VaultViewLoginPageModel.Attachment attachment, Action tappedAction)
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
            public FieldViewCell(VaultViewCipherPage page, VaultViewLoginPageModel.Field field)
                : base(field.Name, field.Value == "true" ? "✓" : "-")
            {
                Init(page, field, null);
            }

            public FieldViewCell(VaultViewCipherPage page, VaultViewLoginPageModel.Field field, bool? a)
                : base(field.Name, field.Value, "clipboard.png")
            {
                Init(page, field, Button1);
            }

            public FieldViewCell(VaultViewCipherPage page, VaultViewLoginPageModel.Field field, bool? a, bool? b)
                : base(field.Name, field.MaskedValue, string.Empty, "clipboard.png")
            {
                Value.FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular",
                    Android: "monospace", WinPhone: "Courier");
                if(Device.RuntimePlatform == Device.iOS)
                {
                    Button1.Margin = new Thickness(10, 0);
                }
                
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

            private void Init(VaultViewCipherPage page, VaultViewLoginPageModel.Field field, ExtendedButton copyButton)
            {
                Value.LineBreakMode = LineBreakMode.WordWrap;
                if(copyButton != null)
                {
                    copyButton.Command = new Command(() => page.Copy(field.Value, field.Name));
                }
            }
        }
    }
}
