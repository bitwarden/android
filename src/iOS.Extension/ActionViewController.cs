using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using Foundation;
using MobileCoreServices;
using Newtonsoft.Json;
using UIKit;
using XLabs.Ioc;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        public ActionViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);

            base.ViewWillAppear(animated);
        }

        public async override void ViewDidLoad()
        {
            base.ViewDidLoad();

            IEnumerable<SiteViewModel> filteredSiteModels = new List< SiteViewModel>();
            DomainName domain;
            if(Context.Url?.Host != null && DomainName.TryParse(Context.Url?.Host, out domain))
            {
                var siteService = Resolver.Resolve<ISiteService>();
                var sites = await siteService.GetAllAsync();
                var siteModels = sites.Select(s => new SiteViewModel(s));
                filteredSiteModels = siteModels.Where(s => s.Domain != null && s.Domain.BaseDomain == domain.BaseDomain);
            }

            tableView.RowHeight = UITableView.AutomaticDimension;
            tableView.EstimatedRowHeight = 44;
            tableView.Source = new TableSource(filteredSiteModels, this);
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

        partial void UIBarButtonItem2293_Activated(UIBarButtonItem sender)
        {
            PerformSegue("addSiteSegue", this);
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var addSiteController = navController.TopViewController as AddSiteViewController;
                if(addSiteController != null)
                {
                    addSiteController.Context = Context;
                }
            }
        }

        public class TableSource : UITableViewSource
        {
            private const string CellIdentifier = "TableCell";

            private IEnumerable<SiteViewModel> _tableItems;
            private Context _context;
            private ActionViewController _controller;

            public TableSource(IEnumerable<SiteViewModel> items, ActionViewController controller)
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

                cell.TextLabel.Text = item.Name;
                cell.DetailTextLabel.Text = item.Username;
                cell.DetailTextLabel.TextColor = cell.DetailTextLabel.TintColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);
                return cell;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                var item = _tableItems.ElementAt(indexPath.Row);
                if(item == null)
                {
                    _controller.CompleteRequest(null);
                }

                NSDictionary itemData = null;
                if(_context.ProviderType == UTType.PropertyList)
                {
                    var fillScript = new FillScript(_context.Details, item.Username, item.Password);
                    var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                    var scriptDict = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                    itemData = new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, scriptDict);
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionFindLoginAction)
                {
                    itemData = new NSDictionary(
                        Constants.AppExtensionUsernameKey, item.Username,
                        Constants.AppExtensionPasswordKey, item.Password);
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionFillBrowserAction
                    || _context.ProviderType == Constants.UTTypeAppExtensionFillWebViewAction)
                {
                    var fillScript = new FillScript(_context.Details, item.Username, item.Password);
                    var scriptJson = JsonConvert.SerializeObject(fillScript, new JsonSerializerSettings { NullValueHandling = NullValueHandling.Ignore });
                    itemData = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                }
                else if(_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
                {
                    itemData = new NSDictionary(
                        Constants.AppExtensionUsernameKey, item.Username,
                        Constants.AppExtensionPasswordKey, item.Password);
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
