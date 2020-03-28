using System;
using System.Threading;
using System.Threading.Tasks;
using Foundation;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class ExtensionSearchDelegate : UISearchBarDelegate
    {
        private readonly UITableView _tableView;
        private CancellationTokenSource _filterResultsCancellationTokenSource;

        public ExtensionSearchDelegate(UITableView tableView)
        {
            _tableView = tableView;
        }

        public override void TextChanged(UISearchBar searchBar, string searchText)
        {
            var cts = new CancellationTokenSource();
            Task.Run(() =>
            {
                NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
                {
                    if (!string.IsNullOrWhiteSpace(searchText))
                    {
                        await Task.Delay(300);
                        if (searchText != searchBar.Text)
                        {
                            return;
                        }
                        else
                        {
                            _filterResultsCancellationTokenSource?.Cancel();
                        }
                    }
                    try
                    {
                        ((ExtensionTableSource)_tableView.Source).FilterResults(searchText, cts.Token);
                        _tableView.ReloadData();
                    }
                    catch (OperationCanceledException) { }
                    _filterResultsCancellationTokenSource = cts;
                });
            }, cts.Token);
        }
    }
}