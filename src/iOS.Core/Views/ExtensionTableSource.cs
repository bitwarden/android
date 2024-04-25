using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
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
        public const string CipherLoginCellIdentifier = nameof(CipherLoginTableViewCell);

        protected IEnumerable<CipherViewModel> _allItems = new List<CipherViewModel>();
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

        public IList<CipherViewModel> Items { get; private set; }

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
                .Select(CreateCipherViewModel)
                .ToList();
        }

        public virtual void FilterResults(string searchFilter, CancellationToken ct)
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
                Items = results.Select(CreateCipherViewModel).ToList();
            }

            OnItemsLoaded(searchFilter, ct);
        }

        protected virtual void OnItemsLoaded(string searchFilter, CancellationToken ct) { }

        protected virtual CipherViewModel CreateCipherViewModel(CipherView cipherView) => new CipherViewModel(cipherView);

        public override nint RowsInSection(UITableView tableview, nint section)
        {
            return Items == null || Items.Count() == 0 ? 1 : Items.Count();
        }

        public virtual void RegisterTableViewCells(UITableView tableView)
        {
            tableView.RegisterClassForCellReuse(typeof(CipherLoginTableViewCell), CipherLoginCellIdentifier);
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

            var cell = tableView.DequeueReusableCell(CipherLoginCellIdentifier);
            if (cell is null)
            {
                throw new InvalidOperationException($"The cell {CipherLoginCellIdentifier} has not been registered in the UITableView");
            }
            return cell;
        }

        public override void WillDisplay(UITableView tableView, UITableViewCell cell, NSIndexPath indexPath)
        {
            try
            {
                if (Items == null
                    || !Items.Any()
                    || !(cell is CipherLoginTableViewCell cipherCell))
                {
                    return;
                }

                var item = Items.ElementAtOrDefault(GetIndexForItemAt(tableView, indexPath));
                if (item is null)
                {
                    return;
                }

                cipherCell.SetTitle(item.Name);
                cipherCell.SetSubtitle(GetCipherCellSubtitle(item, indexPath));
                cipherCell.UpdateMainIcon(ShouldUseMainIconAsPasskey(item, indexPath));
                if (item.IsShared)
                {
                    cipherCell.ShowOrganizationIcon();
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        protected virtual int GetIndexForItemAt(UITableView tableView, NSIndexPath indexPath) => indexPath.Row;

        protected virtual bool ShouldUseMainIconAsPasskey(CipherViewModel item, NSIndexPath indexPath) => item.HasFido2Credential;

        protected virtual string GetCipherCellSubtitle(CipherViewModel item, NSIndexPath indexPath) => item.Username;

        public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
        {
            return 55;
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
