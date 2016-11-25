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
    public class VaultViewSitePage : ExtendedContentPage
    {
        private readonly string _siteId;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;
        private readonly IDeviceInfoService _deviceInfo;

        public VaultViewSitePage(string siteId)
        {
            _siteId = siteId;
            _siteService = Resolver.Resolve<ISiteService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();
            _deviceInfo = Resolver.Resolve<IDeviceInfoService>();

            Init();
        }

        private VaultViewSitePageModel Model { get; set; } = new VaultViewSitePageModel();
        private ExtendedTableView Table { get; set; }
        private TableSection SiteInformationSection { get; set; }
        private TableSection NotesSection { get; set; }
        public LabeledValueCell UsernameCell { get; set; }
        public LabeledValueCell PasswordCell { get; set; }
        public LabeledValueCell UriCell { get; set; }

        private void Init()
        {
            ToolbarItems.Add(new EditSiteToolBarItem(this, _siteId));
            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this));
            }

            // Name
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Name);

            // Username
            UsernameCell = new LabeledValueCell(AppResources.Username, button1Text: AppResources.Copy);
            UsernameCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Username);
            UsernameCell.Value.SetBinding<VaultViewSitePageModel>(Label.FontSizeProperty, s => s.UsernameFontSize);
            UsernameCell.Button1.Command = new Command(() => Copy(Model.Username, AppResources.Username));

            // Password
            PasswordCell = new LabeledValueCell(AppResources.Password, button1Text: string.Empty,
                button2Text: AppResources.Copy);
            PasswordCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.MaskedPassword);
            PasswordCell.Value.SetBinding<VaultViewSitePageModel>(Label.FontSizeProperty, s => s.PasswordFontSize);
            PasswordCell.Button1.SetBinding<VaultViewSitePageModel>(Button.ImageProperty, s => s.ShowHideImage);
            if(Device.OS == TargetPlatform.iOS)
            {
                PasswordCell.Button1.Margin = new Thickness(10, 0);
            }
            PasswordCell.Button1.Command = new Command(() => Model.RevealPassword = !Model.RevealPassword);
            PasswordCell.Button2.Command = new Command(() => Copy(Model.Password, AppResources.Password));
            PasswordCell.Value.FontFamily = Device.OnPlatform(iOS: "Courier", Android: "monospace", WinPhone: "Courier");

            // URI
            UriCell = new LabeledValueCell(AppResources.Website, button1Text: AppResources.Launch);
            UriCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.UriHost);
            UriCell.Button1.SetBinding<VaultViewSitePageModel>(IsVisibleProperty, s => s.ShowLaunch);
            UriCell.Button1.Command = new Command(() => Device.OpenUri(new Uri(Model.Uri)));

            // Notes
            var notesCell = new LabeledValueCell();
            notesCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Notes);
            notesCell.Value.LineBreakMode = LineBreakMode.WordWrap;

            SiteInformationSection = new TableSection(AppResources.SiteInformation)
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
                    SiteInformationSection,
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
                PasswordCell.Button2.WidthRequest = 55;
                UsernameCell.Button1.WidthRequest = 55;
                UriCell.Button1.WidthRequest = _deviceInfo.Version < 21 ? 75 : 71;
            }

            Title = AppResources.ViewSite;
            Content = Table;
            BindingContext = Model;
        }

        protected async override void OnAppearing()
        {
            var site = await _siteService.GetByIdAsync(_siteId);
            if(site == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            Model.Update(site);

            if(!Model.ShowUri)
            {
                SiteInformationSection.Remove(UriCell);
            }
            else if(!SiteInformationSection.Contains(UriCell))
            {
                SiteInformationSection.Add(UriCell);
            }

            if(!Model.ShowUsername)
            {
                SiteInformationSection.Remove(UsernameCell);
            }
            else if(!SiteInformationSection.Contains(UsernameCell))
            {
                SiteInformationSection.Add(UsernameCell);
            }

            if(!Model.ShowPassword)
            {
                SiteInformationSection.Remove(PasswordCell);
            }
            else if(!SiteInformationSection.Contains(PasswordCell))
            {
                SiteInformationSection.Add(PasswordCell);
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

        private class EditSiteToolBarItem : ToolbarItem
        {
            private readonly VaultViewSitePage _page;
            private readonly string _siteId;

            public EditSiteToolBarItem(VaultViewSitePage page, string siteId)
            {
                _page = page;
                _siteId = siteId;
                Text = AppResources.Edit;
                Clicked += ClickedItem;
            }

            private async void ClickedItem(object sender, EventArgs e)
            {
                var page = new VaultEditSitePage(_siteId);
                await _page.Navigation.PushForDeviceAsync(page);
            }
        }
    }
}
