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
    public class VaultViewSitePage : ContentPage
    {
        private readonly string _siteId;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;

        public VaultViewSitePage(string siteId)
        {
            _siteId = siteId;
            _siteService = Resolver.Resolve<ISiteService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }

        private VaultViewSitePageModel Model { get; set; } = new VaultViewSitePageModel();
        private ExtendedTableView Table { get; set; }

        private void Init()
        {
            ToolbarItems.Add(new EditSiteToolBarItem(this, _siteId));
            ToolbarItems.Add(new DismissModalToolBarItem(this));

            // Username
            var nameCell = new LabeledValueCell(AppResources.Name);
            nameCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Name);

            // Username
            var usernameCell = new LabeledValueCell(AppResources.Username, copyValue: true);
            usernameCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Username);

            // Password
            var passwordCell = new LabeledValueCell(AppResources.Password, copyValue: true);
            passwordCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Password);

            // URI
            var uriCell = new LabeledValueCell(AppResources.URI, launch: true);
            uriCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Uri);

            // Notes
            var notesCell = new LabeledValueCell(AppResources.Notes);
            notesCell.Value.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Notes);

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = true,
                Root = new TableRoot
                {
                    new TableSection("Site Information")
                    {
                        uriCell,
                        nameCell,
                        usernameCell,
                        passwordCell
                    },
                    new TableSection(AppResources.Notes)
                    {
                        notesCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 70;
            }

            var scrollView = new ScrollView
            {
                Content = Table,
                Orientation = ScrollOrientation.Vertical
            };

            SetBinding(Page.TitleProperty, new Binding("PageTitle"));
            Content = scrollView;
            BindingContext = Model;
        }

        protected override void OnAppearing()
        {
            var site = _siteService.GetByIdAsync(_siteId).GetAwaiter().GetResult();
            if(site == null)
            {
                // TODO: handle error. navigate back? should never happen...
                return;
            }

            Model.Update(site);

            base.OnAppearing();

            // Hack to get table row height binding to update. Better way to do this probably?
            Table.Root.Add(new TableSection { new TextCell() });
            Table.Root.RemoveAt(Table.Root.Count - 1);
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.SuccessToast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
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
                var page = new ExtendedNavigationPage(new VaultEditSitePage(_siteId));
                await _page.Navigation.PushModalAsync(page);
            }
        }
    }
}
