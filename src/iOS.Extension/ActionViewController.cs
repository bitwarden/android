using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using Foundation;
using MobileCoreServices;
using Newtonsoft.Json;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController(IntPtr handle) : base(handle)
        {
        }

        public Context Context { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);

            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();

            List<Tuple<string, string>> sites = new List<Tuple<string, string>>();
            for(int i = 1; i <= 100; i++)
            {
                sites.Add(new Tuple<string, string>("Site " + i, "Username " + i));
            }

            tableView.Source = new TableSource(sites, this);
            AutomaticallyAdjustsScrollViewInsets = false;
        }

        partial void CancelClicked(UIBarButtonItem sender)
        {
            CompleteRequest(null);
        }

        private void CompleteRequest(NSDictionary itemData)
        {
            var resultsProvider = new NSItemProvider(itemData, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };

            Context.ExtContext.CompleteRequest(returningItems, null);
        }

        public class TableSource : UITableViewSource
        {
            private const string CellIdentifier = "TableCell";

            private IEnumerable<Tuple<string, string>> _tableItems;
            private Context _context;
            private ActionViewController _controller;

            public TableSource(IEnumerable<Tuple<string, string>> items, ActionViewController controller)
            {
                _tableItems = items;
                _context = controller.Context;
                _controller = controller;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                return _tableItems.Count();
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                var cell = tableView.DequeueReusableCell(CellIdentifier);
                var item = _tableItems.ElementAt(indexPath.Row);

                // if there are no cells to reuse, create a new one
                if(cell == null)
                {
                    cell = new UITableViewCell(UITableViewCellStyle.Subtitle, CellIdentifier);
                }

                cell.TextLabel.Text = item.Item1;
                cell.DetailTextLabel.Text = item.Item2;
                return cell;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                NSDictionary itemData = null;
                if(_context.ProviderType == UTType.PropertyList)
                {
                    var fillScript = new FillScript(_context.Details);
                    var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                    var scriptDict = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                    itemData = new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, scriptDict);
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionFindLoginAction)
                {
                    itemData = new NSDictionary(
                        Constants.AppExtensionUsernameKey, "me@example.com",
                        Constants.AppExtensionPasswordKey, "mypassword");
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionFillBrowserAction
                    || _context.ProviderType == Constants.UTTypeAppExtensionFillWebViewAction)
                {
                    var fillScript = new FillScript(_context.Details);
                    var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                    itemData = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
                {
                    itemData = new NSDictionary(
                        Constants.AppExtensionUsernameKey, "me@example.com",
                        Constants.AppExtensionPasswordKey, "mypassword");
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionChangePasswordAction)
                {
                    itemData = new NSDictionary(
                        Constants.AppExtensionPasswordKey, "mynewpassword",
                        Constants.AppExtensionOldPasswordKey, "myoldpassword");
                }

                Debug.WriteLine("BW LOG, itemData: " + itemData);

                _controller.CompleteRequest(itemData);
            }
        }
    }
}
