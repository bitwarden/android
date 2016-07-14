using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.iOS.Core.Views;
using Bit.iOS.Extension.Models;
using Foundation;
using UIKit;
using XLabs.Ioc;
using Bit.App;
using Plugin.Connectivity.Abstractions;
using Bit.iOS.Core.Utilities;
using Plugin.Settings.Abstractions;

namespace Bit.iOS.Extension
{
    public partial class PasswordGeneratorViewController : UIViewController
    {
        private IPasswordGenerationService _passwordGenerationService;
        private ISettings _settings;

        public PasswordGeneratorViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public SiteAddViewController Parent { get; set; }
        public UITableViewController OptionsTableViewController { get; set; }
        public SwitchTableViewCell UppercaseCell { get; set; } = new SwitchTableViewCell("A-Z");
        public SwitchTableViewCell LowercaseCell { get; set; } = new SwitchTableViewCell("a-z");
        public SwitchTableViewCell NumbersCell { get; set; } = new SwitchTableViewCell("0-9");
        public SwitchTableViewCell SpecialCell { get; set; } = new SwitchTableViewCell("!@#$%^&*");

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            _passwordGenerationService = Resolver.Resolve<IPasswordGenerationService>();
            _settings = Resolver.Resolve<ISettings>();

            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            PasswordLabel.Text = _passwordGenerationService.GeneratePassword();
            PasswordLabel.Font = UIFont.FromName("Courier", 17);

            var controller = ChildViewControllers.LastOrDefault();
            if(controller != null)
            {
                OptionsTableViewController = controller as UITableViewController;
            }

            if(OptionsTableViewController != null)
            {
                OptionsTableViewController.TableView.RowHeight = UITableView.AutomaticDimension;
                OptionsTableViewController.TableView.EstimatedRowHeight = 70;
                OptionsTableViewController.TableView.Source = new TableSource(this);
                OptionsTableViewController.TableView.AllowsSelection = true;
                OptionsTableViewController.View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);
            }

            base.ViewDidLoad();
        }

        partial void SelectBarButton_Activated(UIBarButtonItem sender)
        {
            throw new NotImplementedException();
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            throw new NotImplementedException();
        }

        public class TableSource : UITableViewSource
        {
            private PasswordGeneratorViewController _controller;

            public TableSource(PasswordGeneratorViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    var cell = new UITableViewCell();
                    cell.TextLabel.TextColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);
                    if(indexPath.Row == 0)
                    {
                        cell.TextLabel.Text = "Regenerate Password";
                    }
                    else if(indexPath.Row == 1)
                    {
                        cell.TextLabel.Text = "Copy Password";
                    }
                    return cell;
                }

                if(indexPath.Row == 0)
                {
                    // TODO: Length slider
                }
                else if(indexPath.Row == 1)
                {
                    return _controller.UppercaseCell;
                }
                else if(indexPath.Row == 2)
                {
                    return _controller.LowercaseCell;
                }
                else if(indexPath.Row == 3)
                {
                    return _controller.NumbersCell;
                }
                else if(indexPath.Row == 4)
                {
                    return _controller.SpecialCell;
                }
                else if(indexPath.Row == 5)
                {
                    // TODO: Min numbers stepper
                }
                else if(indexPath.Row == 6)
                {
                    // TODO: Min special stepper
                }

                return new UITableViewCell();
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                return 2;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if(section == 0)
                {
                    return 2;
                }

                return 7;
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                return UITableView.AutomaticDimension;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                if(section == 1)
                {
                    return "Options";
                }

                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    if(indexPath.Row == 0)
                    {
                        _controller.PasswordLabel.Text = _controller._passwordGenerationService.GeneratePassword();
                    }
                    else if(indexPath.Row == 1)
                    {
                        // TODO: copy to clipboard
                    }
                }

                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
            }
        }
    }
}
