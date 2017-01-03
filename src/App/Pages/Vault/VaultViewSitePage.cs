using System;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultViewLoginPage : ExtendedContentPage
    {
        private readonly string _loginId;
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;

        public VaultViewLoginPage(string loginId)
        {
            _loginId = loginId;
            _loginService = Resolver.Resolve<ILoginService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }

        private VaultViewLoginPageModel Model { get; set; } = new VaultViewLoginPageModel();
        private ExtendedTableView Table { get; set; }
        private TableSection LoginInformationSection { get; set; }
        private TableSection NotesSection { get; set; }
        public LabeledValueCell UsernameCell { get; set; }
        public LabeledValueCell PasswordCell { get; set; }
        public LabeledValueCell UriCell { get; set; }

        private void Init()
        {
            ToolbarItems.Add(new EditLoginToolBarItem(this, _loginId));
            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this));
            }

            // Name
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding<VaultViewLoginPageModel>(Label.TextProperty, s => s.Name);

            // Username
            UsernameCell = new LabeledValueCell(AppResources.Username, button1Text: AppResources.Copy);
            UsernameCell.Value.SetBinding<VaultViewLoginPageModel>(Label.TextProperty, s => s.Username);
            UsernameCell.Value.SetBinding<VaultViewLoginPageModel>(Label.FontSizeProperty, s => s.UsernameFontSize);
            UsernameCell.Button1.Command = new Command(() => Copy(Model.Username, AppResources.Username));

            // Password
            PasswordCell = new LabeledValueCell(AppResources.Password, button1Text: string.Empty,
                button2Text: AppResources.Copy);
            PasswordCell.Value.SetBinding<VaultViewLoginPageModel>(Label.TextProperty, s => s.MaskedPassword);
            PasswordCell.Value.SetBinding<VaultViewLoginPageModel>(Label.FontSizeProperty, s => s.PasswordFontSize);
            PasswordCell.Button1.SetBinding<VaultViewLoginPageModel>(Button.ImageProperty, s => s.ShowHideImage);
            if(Device.OS == TargetPlatform.iOS)
            {
                PasswordCell.Button1.Margin = new Thickness(10, 0);
            }
            PasswordCell.Button1.Command = new Command(() => Model.RevealPassword = !Model.RevealPassword);
            PasswordCell.Button2.Command = new Command(() => Copy(Model.Password, AppResources.Password));
            PasswordCell.Value.FontFamily = Device.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            // URI
            UriCell = new LabeledValueCell(AppResources.Website, button1Text: AppResources.Launch);
            UriCell.Value.SetBinding<VaultViewLoginPageModel>(Label.TextProperty, s => s.UriHost);
            UriCell.Button1.SetBinding<VaultViewLoginPageModel>(IsVisibleProperty, s => s.ShowLaunch);
            UriCell.Button1.Command = new Command(() => Device.OpenUri(new Uri(Model.Uri)));

            // Notes
            var notesCell = new LabeledValueCell();
            notesCell.Value.SetBinding<VaultViewLoginPageModel>(Label.TextProperty, s => s.Notes);
            notesCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            LoginInformationSection = new TableSection(AppResources.LoginInformation)
            {
                nameCell
            };

            NotesSection = new TableSection(AppResources.Notes)
            {
                notesCell
            };

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    LoginInformationSection,
                    NotesSection
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 70;
            }
            else if(Device.OS == TargetPlatform.Android)
            {
                // NOTE: This is going to cause problems with i18n strings since various languages have difference string sizes
                PasswordCell.Button1.WidthRequest = 40;
                PasswordCell.Button2.WidthRequest = 59;
                UsernameCell.Button1.WidthRequest = 59;
                UriCell.Button1.WidthRequest = 75;
            }

            Title = AppResources.ViewLogin;
            Content = Table;
            BindingContext = Model;
        }

        protected async override void OnAppearing()
        {
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

            base.OnAppearing();
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private class EditLoginToolBarItem : ToolbarItem
        {
            private readonly VaultViewLoginPage _page;
            private readonly string _loginId;

            public EditLoginToolBarItem(VaultViewLoginPage page, string loginId)
            {
                _page = page;
                _loginId = loginId;
                Text = AppResources.Edit;
                Clicked += ClickedItem;
            }

            private async void ClickedItem(object sender, EventArgs e)
            {
                var page = new VaultEditLoginPage(_loginId);
                await _page.Navigation.PushForDeviceAsync(page);
            }
        }
    }
}
