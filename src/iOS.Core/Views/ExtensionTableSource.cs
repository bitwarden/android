using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.iOS.Core.Models;
using Foundation;
using Plugin.Settings.Abstractions;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using UIKit;
using XLabs.Ioc;

namespace Bit.iOS.Core.Views
{
    public class ExtensionTableSource : UITableViewSource
    {
        private const string CellIdentifier = "TableCell";

        private IEnumerable<CipherViewModel> _allItems = new List<CipherViewModel>();
        protected IEnumerable<CipherViewModel> _tableItems = new List<CipherViewModel>();
        protected ICipherService _cipherService;
        protected ISettings _settings;
        private bool _accessPremium;
        private AppExtensionContext _context;
        private UIViewController _controller;

        public ExtensionTableSource(AppExtensionContext context, UIViewController controller)
        {
            _accessPremium = Helpers.CanAccessPremium();
            _cipherService = Resolver.Resolve<ICipherService>();
            _settings = Resolver.Resolve<ISettings>();
            _context = context;
            _controller = controller;
        }

        public async Task LoadItemsAsync(bool urlFilter = true, string searchFilter = null)
        {
            var combinedLogins = new List<Cipher>();

            if(urlFilter)
            {
                var logins = await _cipherService.GetAllAsync(_context.UrlString);
                if(logins?.Item1 != null)
                {
                    combinedLogins.AddRange(logins.Item1);
                }
                if(logins?.Item2 != null)
                {
                    combinedLogins.AddRange(logins.Item2);
                }
            }
            else
            {
                var logins = await _cipherService.GetAllAsync();
                combinedLogins.AddRange(logins);
            }

            _allItems = combinedLogins
                .Where(c => c.Type == App.Enums.CipherType.Login)
                .Select(s => new CipherViewModel(s))
                .OrderBy(s => s.Name)
                .ThenBy(s => s.Username)
                .ToList() ?? new List<CipherViewModel>();
            FilterResults(searchFilter, new CancellationToken());
        }

        public void FilterResults(string searchFilter, CancellationToken ct)
        {
            ct.ThrowIfCancellationRequested();

            if(string.IsNullOrWhiteSpace(searchFilter))
            {
                _tableItems = _allItems.ToList();
            }
            else
            {
                searchFilter = searchFilter.ToLower();
                _tableItems = _allItems
                    .Where(s => s.Name.ToLower().Contains(searchFilter) ||
                        (s.Username?.ToLower().Contains(searchFilter) ?? false) ||
                        (s.Uris?.FirstOrDefault()?.Uri.ToLower().Contains(searchFilter) ?? false))
                    .TakeWhile(s => !ct.IsCancellationRequested)
                    .ToArray();
            }
        }

        public IEnumerable<CipherViewModel> TableItems { get; set; }

        public override nint RowsInSection(UITableView tableview, nint section)
        {
            return _tableItems == null || _tableItems.Count() == 0 ? 1 : _tableItems.Count();
        }

        public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
        {
            if(_tableItems == null || _tableItems.Count() == 0)
            {
                var noDataCell = new UITableViewCell(UITableViewCellStyle.Default, "NoDataCell");
                noDataCell.TextLabel.Text = AppResources.NoItemsTap;
                noDataCell.TextLabel.TextAlignment = UITextAlignment.Center;
                noDataCell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                noDataCell.TextLabel.Lines = 0;
                return noDataCell;
            }

            var cell = tableView.DequeueReusableCell(CellIdentifier);

            // if there are no cells to reuse, create a new one
            if(cell == null)
            {
                Debug.WriteLine("BW Log, Make new cell for list.");
                cell = new UITableViewCell(UITableViewCellStyle.Subtitle, CellIdentifier);
                cell.DetailTextLabel.TextColor = cell.DetailTextLabel.TintColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);
            }
            return cell;
        }

        public override void WillDisplay(UITableView tableView, UITableViewCell cell, NSIndexPath indexPath)
        {
            if(_tableItems == null || _tableItems.Count() == 0 || cell == null)
            {
                return;
            }

            var item = _tableItems.ElementAt(indexPath.Row);
            cell.TextLabel.Text = item.Name;
            cell.DetailTextLabel.Text = item.Username;
        }

        protected string GetTotp(CipherViewModel item)
        {
            string totp = null;
            if(_accessPremium)
            {
                if(item != null && !string.IsNullOrWhiteSpace(item.Totp.Value))
                {
                    totp = Crypto.Totp(item.Totp.Value);
                }
            }

            return totp;
        }
    }
}
