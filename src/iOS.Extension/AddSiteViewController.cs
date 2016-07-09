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

namespace Bit.iOS.Extension
{
    public partial class AddSiteViewController : UITableViewController
    {
        public AddSiteViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public FormEntryTableViewCell NameCell { get; set; } = new FormEntryTableViewCell(AppResources.Name);
        public FormEntryTableViewCell UriCell { get; set; } = new FormEntryTableViewCell(AppResources.URI);
        public FormEntryTableViewCell UsernameCell { get; set; } = new FormEntryTableViewCell(AppResources.Username);
        public FormEntryTableViewCell PasswordCell { get; set; } = new FormEntryTableViewCell(AppResources.Password);
        public UITableViewCell GeneratePasswordCell { get; set; } = new UITableViewCell(UITableViewCellStyle.Subtitle, "GeneratePasswordCell");
        public SwitchTableViewCell FavoriteCell { get; set; } = new SwitchTableViewCell("Favorite");
        public FormEntryTableViewCell NotesCell { get; set; } = new FormEntryTableViewCell(useTextView: true, height: 90);

        public override void ViewWillAppear(bool animated)
        {
            UINavigationBar.Appearance.ShadowImage = new UIImage();
            UINavigationBar.Appearance.SetBackgroundImage(new UIImage(), UIBarMetrics.Default);
            base.ViewWillAppear(animated);
        }

        public override void ViewDidLoad()
        {
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            NameCell.TextField.Text = Context.Url.Host;
            NameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            NameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UriCell.TextField.BecomeFirstResponder();
                return true;
            };

            UriCell.TextField.Text = Context.Url.ToString();
            UriCell.TextField.KeyboardType = UIKeyboardType.Url;
            UriCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UriCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UsernameCell.TextField.BecomeFirstResponder();
                return true;
            };

            UsernameCell.TextField.BecomeFirstResponder();
            UsernameCell.TextField.AutocapitalizationType = UITextAutocapitalizationType.None;
            UsernameCell.TextField.AutocorrectionType = UITextAutocorrectionType.No;
            UsernameCell.TextField.SpellCheckingType = UITextSpellCheckingType.No;
            UsernameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UsernameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                PasswordCell.TextField.BecomeFirstResponder();
                return true;
            };

            PasswordCell.TextField.SecureTextEntry = true;
            PasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            PasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                NotesCell.TextView.BecomeFirstResponder();
                return true;
            };

            GeneratePasswordCell.TextLabel.Text = "Generate Password";
            GeneratePasswordCell.Accessory = UITableViewCellAccessory.DisclosureIndicator;

            tableView.RowHeight = UITableView.AutomaticDimension;
            tableView.EstimatedRowHeight = 70;
            tableView.Source = new TableSource(this);

            base.ViewDidLoad();
        }

        partial void UIBarButtonItem2289_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        partial void UIBarButtonItem2290_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        public class TableSource : UITableViewSource
        {
            private AddSiteViewController _controller;

            public TableSource(AddSiteViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    if(indexPath.Row == 0)
                    {
                        return _controller.NameCell;
                    }
                    else if(indexPath.Row == 1)
                    {
                        return _controller.UriCell;
                    }
                    else if(indexPath.Row == 2)
                    {
                        return _controller.UsernameCell;
                    }
                    else if(indexPath.Row == 3)
                    {
                        return _controller.PasswordCell;
                    }
                    else if(indexPath.Row == 4)
                    {
                        return _controller.GeneratePasswordCell;
                    }
                }
                else if(indexPath.Section == 1)
                {
                    if(indexPath.Row == 1)
                    {
                        return _controller.FavoriteCell;
                    }
                }
                else if(indexPath.Section == 2)
                {
                    return _controller.NotesCell;
                }

                return new UITableViewCell();
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                return 3;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if(section == 0)
                {
                    return 5;
                }
                else if(section == 1)
                {
                    return 2;
                }
                else
                {
                    return 1;
                }
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                return UITableView.AutomaticDimension;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                if(section == 0)
                {
                    return "Site Information";
                }
                else if(section == 2)
                {
                    return "Notes";
                }

                return null;
            }
        }
    }
}
