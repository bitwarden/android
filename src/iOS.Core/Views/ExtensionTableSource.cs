using System.Diagnostics;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Models;
using Bit.iOS.Core.Utilities;
using Foundation;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class ExtensionTableSource : ExtendedUITableViewSource
    {
        private const string CellIdentifier = "TableCell";

        private IEnumerable<CipherViewModel> _allItems = new List<CipherViewModel>();
        protected ICipherService _cipherService;
        protected ITotpService _totpService;
        protected IStateService _stateService;
        protected ISearchService _searchService;
        protected AppExtensionContext _context;
        protected UIViewController _controller;

        public ExtensionTableSource(AppExtensionContext context, UIViewController controller)
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            _context = context;
            _controller = controller;

            Items = new List<CipherViewModel>();
        }

        public IEnumerable<CipherViewModel> Items { get; private set; }

        public virtual async Task LoadAsync(bool urlFilter = true, string searchFilter = null)
        {
            _allItems = await LoadItemsAsync(urlFilter, searchFilter);
            FilterResults(searchFilter, new CancellationToken());
        }

        protected virtual async Task<IEnumerable<CipherViewModel>> LoadItemsAsync(bool urlFilter = true, string? searchFilter = null)
        {
            var combinedLogins = new List<CipherView>();

            if (urlFilter)
            {
                var logins = await _cipherService.GetAllDecryptedByUrlAsync(_context.UrlString);
                if (logins?.Item1 != null)
                {
                    combinedLogins.AddRange(logins.Item1);
                }
                if (logins?.Item2 != null)
                {
                    combinedLogins.AddRange(logins.Item2);
                }
            }
            else
            {
                var logins = await _cipherService.GetAllDecryptedAsync();
                combinedLogins.AddRange(logins);
            }

            return combinedLogins
                .Where(c => c.Type == Bit.Core.Enums.CipherType.Login && !c.IsDeleted)
                .Select(s => new CipherViewModel(s))
                .ToList();
        }

        public void FilterResults(string searchFilter, CancellationToken ct)
        {
            ct.ThrowIfCancellationRequested();

            if (string.IsNullOrWhiteSpace(searchFilter))
            {
                Items = _allItems.ToList();
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                var results = _searchService.SearchCiphersAsync(searchFilter,
                    c => c.Type == Bit.Core.Enums.CipherType.Login && !c.IsDeleted, null, ct)
                    .GetAwaiter().GetResult();
                Items = results.Select(s => new CipherViewModel(s)).ToArray();
            }
        }

        //public IEnumerable<CipherViewModel> TableItems { get; set; }

        public override nint RowsInSection(UITableView tableview, nint section)
        {
            return Items == null || Items.Count() == 0 ? 1 : Items.Count();
        }

        public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
        {
            if (Items == null || Items.Count() == 0)
            {
                var noDataCell = new ExtendedUITableViewCell(UITableViewCellStyle.Default, "NoDataCell");
                noDataCell.TextLabel.Text = AppResources.NoItemsTap;
                noDataCell.TextLabel.TextAlignment = UITextAlignment.Center;
                noDataCell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                noDataCell.TextLabel.Lines = 0;
                noDataCell.TextLabel.TextColor = ThemeHelpers.TextColor;
                return noDataCell;
            }

            var cell = tableView.DequeueReusableCell(CellIdentifier);

            // if there are no cells to reuse, create a new one
            if (cell == null)
            {
                Debug.WriteLine("BW Log, Make new cell for list.");
                cell = new ExtendedUITableViewCell(UITableViewCellStyle.Subtitle, CellIdentifier);
                cell.TextLabel.TextColor = cell.TextLabel.TintColor = ThemeHelpers.TextColor;
                cell.DetailTextLabel.TextColor = cell.DetailTextLabel.TintColor = ThemeHelpers.MutedColor;
            }
            return cell;
        }

        public override void WillDisplay(UITableView tableView, UITableViewCell cell, NSIndexPath indexPath)
        {
            if (Items == null
                || !Items.Any()
                || cell?.TextLabel == null
                || cell.DetailTextLabel == null)
            {
                return;
            }

            var item = Items.ElementAt(indexPath.Row);
            cell.TextLabel.Text = item.Name;
            cell.DetailTextLabel.Text = item.Username;
        }

        public async Task<string?> GetTotpAsync(CipherViewModel item)
        {
            string? totp = null;
            var accessPremium = await _stateService.CanAccessPremiumAsync();
            if (accessPremium || (item?.CipherView.OrganizationUseTotp ?? false))
            {
                if (item != null && !string.IsNullOrWhiteSpace(item.Totp))
                {
                    totp = await _totpService.GetCodeAsync(item.Totp);
                }
            }
            return totp;
        }
    }
}
