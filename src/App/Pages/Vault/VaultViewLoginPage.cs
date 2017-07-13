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

namespace Bit.App.Pages
{
    public class VaultViewLoginPage : ExtendedContentPage
    {
        private readonly string _loginId;
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IDeviceActionService _deviceActionService;
        private readonly ITokenService _tokenService;

        public VaultViewLoginPage(string loginId)
        {
            _loginId = loginId;
            _loginService = Resolver.Resolve<ILoginService>();
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
        public LabeledValueCell UsernameCell { get; set; }
        public LabeledValueCell PasswordCell { get; set; }
        public LabeledValueCell UriCell { get; set; }
        public LabeledValueCell NotesCell { get; set; }
        public LabeledValueCell TotpCodeCell { get; set; }
        private EditLoginToolBarItem EditItem { get; set; }
        public List<AttachmentViewCell> AttachmentCells { get; set; }

        private void Init()
        {
            EditItem = new EditLoginToolBarItem(this, _loginId);
            ToolbarItems.Add(EditItem);
            if(Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this));
            }

            // Name
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Name));

            // Username
            UsernameCell = new LabeledValueCell(AppResources.Username, button1Text: AppResources.Copy);
            UsernameCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Username));
            UsernameCell.Value.SetBinding(Label.FontSizeProperty, nameof(VaultViewLoginPageModel.UsernameFontSize));
            UsernameCell.Button1.Command = new Command(() => Copy(Model.Username, AppResources.Username));

            // Password
            PasswordCell = new LabeledValueCell(AppResources.Password, button1Text: string.Empty,
                button2Text: AppResources.Copy);
            PasswordCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.MaskedPassword));
            PasswordCell.Value.SetBinding(Label.FontSizeProperty, nameof(VaultViewLoginPageModel.PasswordFontSize));
            PasswordCell.Button1.SetBinding(Button.ImageProperty, nameof(VaultViewLoginPageModel.ShowHideImage));
            if(Device.RuntimePlatform == Device.iOS)
            {
                PasswordCell.Button1.Margin = new Thickness(10, 0);
            }
            PasswordCell.Button1.Command = new Command(() => Model.RevealPassword = !Model.RevealPassword);
            PasswordCell.Button2.Command = new Command(() => Copy(Model.Password, AppResources.Password));
            PasswordCell.Value.FontFamily = Helpers.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            // URI
            UriCell = new LabeledValueCell(AppResources.Website, button1Text: AppResources.Launch);
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
            TotpCodeCell = new LabeledValueCell(AppResources.VerificationCodeTotp, button1Text: AppResources.Copy, subText: "--");
            TotpCodeCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.TotpCodeFormatted));
            TotpCodeCell.Value.SetBinding(Label.TextColorProperty, nameof(VaultViewLoginPageModel.TotpColor));
            TotpCodeCell.Button1.Command = new Command(() => Copy(Model.TotpCode, AppResources.VerificationCodeTotp));
            TotpCodeCell.Sub.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.TotpSecond));
            TotpCodeCell.Sub.SetBinding(Label.TextColorProperty, nameof(VaultViewLoginPageModel.TotpColor));
            TotpCodeCell.Value.FontFamily = Helpers.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            // Notes
            NotesCell = new LabeledValueCell();
            NotesCell.Value.SetBinding(Label.TextProperty, nameof(VaultViewLoginPageModel.Notes));
            NotesCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            LoginInformationSection = new TableSection(AppResources.LoginInformation)
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
            else if(Device.RuntimePlatform == Device.Android)
            {
                // NOTE: This is going to cause problems with i18n strings since various languages have difference string sizes
                PasswordCell.Button1.WidthRequest = 40;
                PasswordCell.Button2.WidthRequest = 59;
                UsernameCell.Button1.WidthRequest = 59;
                TotpCodeCell.Button1.WidthRequest = 59;
                UriCell.Button1.WidthRequest = 75;
            }

            Title = AppResources.ViewLogin;
            Content = Table;
            BindingContext = Model;
        }

        protected async override void OnAppearing()
        {
            NotesCell.Tapped += NotesCell_Tapped;
            EditItem.InitEvents();

            var login = await _loginService.GetByIdAsync(_loginId);
            if(login == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            Model.Update(login);

            if(!Model.ShowUri)
            {
                LoginInformationSection.Remove(UriCell);
            }
            else if(!LoginInformationSection.Contains(UriCell))
            {
                LoginInformationSection.Add(UriCell);
            }

            if(!Model.ShowUsername)
            {
                LoginInformationSection.Remove(UsernameCell);
            }
            else if(!LoginInformationSection.Contains(UsernameCell))
            {
                LoginInformationSection.Add(UsernameCell);
            }

            if(!Model.ShowPassword)
            {
                LoginInformationSection.Remove(PasswordCell);
            }
            else if(!LoginInformationSection.Contains(PasswordCell))
            {
                LoginInformationSection.Add(PasswordCell);
            }

            if(!Model.ShowNotes)
            {
                Table.Root.Remove(NotesSection);
            }
            else if(!Table.Root.Contains(NotesSection))
            {
                Table.Root.Add(NotesSection);
            }

            CleanupAttachmentCells();
            if(!Model.ShowAttachments && Table.Root.Contains(AttachmentsSection))
            {
                Table.Root.Remove(AttachmentsSection);
            }
            else if(Model.ShowAttachments && !Table.Root.Contains(AttachmentsSection))
            {
                AttachmentsSection = new TableSection(AppResources.Attachments);
                AttachmentCells = new List<AttachmentViewCell>();
                foreach(var attachment in Model.Attachments)
                {
                    var attachmentCell = new AttachmentViewCell(attachment, async () =>
                    {
                        await OpenAttachmentAsync(login, attachment);
                    });
                    AttachmentCells.Add(attachmentCell);
                    AttachmentsSection.Add(attachmentCell);
                    attachmentCell.InitEvents();
                }
                Table.Root.Add(AttachmentsSection);
            }

            // Totp
            var removeTotp = login.Totp == null || (!_tokenService.TokenPremium && !login.OrganizationUseTotp);
            if(!removeTotp)
            {
                var totpKey = login.Totp.Decrypt(login.OrganizationId);
                removeTotp = string.IsNullOrWhiteSpace(totpKey);
                if(!removeTotp)
                {
                    Model.TotpCode = Crypto.Totp(totpKey);
                    removeTotp = string.IsNullOrWhiteSpace(Model.TotpCode);
                    if(!removeTotp)
                    {
                        TotpTick(totpKey);
                        Device.StartTimer(new TimeSpan(0, 0, 1), () =>
                        {
                            TotpTick(totpKey);
                            return true;
                        });

                        if(!LoginInformationSection.Contains(TotpCodeCell))
                        {
                            LoginInformationSection.Add(TotpCodeCell);
                        }
                    }
                }
            }

            if(removeTotp && LoginInformationSection.Contains(TotpCodeCell))
            {
                LoginInformationSection.Remove(TotpCodeCell);
            }

            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
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

        private async Task OpenAttachmentAsync(Login login, VaultViewLoginPageModel.Attachment attachment)
        {
            // 20 MB warning
            if(attachment.Size >= 20971520 && !(await _userDialogs.ConfirmAsync(
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
            var data = await _loginService.DownloadAndDecryptAttachmentAsync(attachment.Url, login.OrganizationId);
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
            private readonly VaultViewLoginPage _page;
            private readonly string _loginId;

            public EditLoginToolBarItem(VaultViewLoginPage page, string loginId)
            {
                _page = page;
                _loginId = loginId;
                Text = AppResources.Edit;
                ClickAction = async () => await ClickedItem();
            }

            private async Task ClickedItem()
            {
                var page = new VaultEditLoginPage(_loginId);
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
                Icon.Source = "download";
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
    }
}
