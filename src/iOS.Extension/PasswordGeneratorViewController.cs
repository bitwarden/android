using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.iOS.Core.Views;
using Bit.iOS.Extension.Models;
using Foundation;
using UIKit;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using CoreGraphics;
using Bit.App;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Controllers;
using Bit.App.Resources;

namespace Bit.iOS.Extension
{
    public partial class PasswordGeneratorViewController : ExtendedUIViewController
    {
        private IPasswordGenerationService _passwordGenerationService;
        private ISettings _settings;
        private IGoogleAnalyticsService _googleAnalyticsService;

        public PasswordGeneratorViewController(IntPtr handle) : base(handle)
        { }

        public Context Context { get; set; }
        public LoginAddViewController Parent { get; set; }
        public UITableViewController OptionsTableViewController { get; set; }
        public SwitchTableViewCell UppercaseCell { get; set; } = new SwitchTableViewCell("A-Z");
        public SwitchTableViewCell LowercaseCell { get; set; } = new SwitchTableViewCell("a-z");
        public SwitchTableViewCell NumbersCell { get; set; } = new SwitchTableViewCell("0-9");
        public SwitchTableViewCell SpecialCell { get; set; } = new SwitchTableViewCell("!@#$%^&*");
        public StepperTableViewCell MinNumbersCell { get; set; } = new StepperTableViewCell(AppResources.MinNumbers, 1, 0, 5, 1);
        public StepperTableViewCell MinSpecialCell { get; set; } = new StepperTableViewCell(AppResources.MinSpecial, 1, 0, 5, 1);
        public SliderTableViewCell LengthCell { get; set; } = new SliderTableViewCell(AppResources.Length, 10, 5, 64);

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
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            NavItem.Title = AppResources.PasswordGenerator;
            CancelBarButton.Title = AppResources.Cancel;
            SelectBarButton.Title = AppResources.Select;
            View.BackgroundColor = new UIColor(red: 0.94f, green: 0.94f, blue: 0.96f, alpha: 1.0f);

            var descriptor = UIFontDescriptor.PreferredBody;
            PasswordLabel.Font = UIFont.FromName("Menlo-Regular", descriptor.PointSize * 1.3f);
            PasswordLabel.LineBreakMode = UILineBreakMode.TailTruncation;
            PasswordLabel.Lines = 1;
            PasswordLabel.AdjustsFontSizeToFitWidth = false;

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

            UppercaseCell.Switch.On = _settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, true);
            LowercaseCell.Switch.On = _settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, true);
            SpecialCell.Switch.On = _settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, true);
            NumbersCell.Switch.On = _settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, true);
            MinNumbersCell.Value = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1);
            MinSpecialCell.Value = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1);
            LengthCell.Value = _settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10);

            UppercaseCell.ValueChanged += Options_ValueChanged;
            LowercaseCell.ValueChanged += Options_ValueChanged;
            NumbersCell.ValueChanged += Options_ValueChanged;
            SpecialCell.ValueChanged += Options_ValueChanged;
            MinNumbersCell.ValueChanged += Options_ValueChanged;
            MinSpecialCell.ValueChanged += Options_ValueChanged;
            LengthCell.ValueChanged += Options_ValueChanged;

            // Adjust based on context password options
            if(Context.PasswordOptions != null)
            {
                if(Context.PasswordOptions.RequireDigits)
                {
                    NumbersCell.Switch.On = true;
                    NumbersCell.Switch.Enabled = false;

                    if(MinNumbersCell.Value < 1)
                    {
                        MinNumbersCell.Value = 1;
                    }

                    MinNumbersCell.Stepper.MinimumValue = 1;
                }

                if(Context.PasswordOptions.RequireSymbols)
                {
                    SpecialCell.Switch.On = true;
                    SpecialCell.Switch.Enabled = false;

                    if(MinSpecialCell.Value < 1)
                    {
                        MinSpecialCell.Value = 1;
                    }

                    MinSpecialCell.Stepper.MinimumValue = 1;
                }

                if(Context.PasswordOptions.MinLength < Context.PasswordOptions.MaxLength)
                {
                    if(Context.PasswordOptions.MinLength > 0 && Context.PasswordOptions.MinLength > LengthCell.Slider.MinValue)
                    {
                        if(LengthCell.Value < Context.PasswordOptions.MinLength)
                        {
                            LengthCell.Slider.Value = Context.PasswordOptions.MinLength;
                        }

                        LengthCell.Slider.MinValue = Context.PasswordOptions.MinLength;
                    }

                    if(Context.PasswordOptions.MaxLength > 5 && Context.PasswordOptions.MaxLength < LengthCell.Slider.MaxValue)
                    {
                        if(LengthCell.Value > Context.PasswordOptions.MaxLength)
                        {
                            LengthCell.Slider.Value = Context.PasswordOptions.MaxLength;
                        }

                        LengthCell.Slider.MaxValue = Context.PasswordOptions.MaxLength;
                    }
                }
            }

            GeneratePassword();
            _googleAnalyticsService.TrackExtensionEvent("GeneratedPassword");
            base.ViewDidLoad();
        }

        private void Options_ValueChanged(object sender, EventArgs e)
        {
            if(InvalidState())
            {
                LowercaseCell.Switch.On = true;
            }

            GeneratePassword();
        }

        private bool InvalidState()
        {
            return !LowercaseCell.Switch.On && !UppercaseCell.Switch.On && !NumbersCell.Switch.On && !SpecialCell.Switch.On;
        }

        partial void SelectBarButton_Activated(UIBarButtonItem sender)
        {
            _googleAnalyticsService.TrackExtensionEvent("SelectedGeneratedPassword");
            DismissViewController(true, () =>
            {
                Parent.PasswordCell.TextField.Text = PasswordLabel.Text;
            });
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }

        private void GeneratePassword()
        {
            PasswordLabel.Text = _passwordGenerationService.GeneratePassword(
                length: LengthCell.Value,
                uppercase: UppercaseCell.Switch.On,
                lowercase: LowercaseCell.Switch.On,
                numbers: NumbersCell.Switch.On,
                special: SpecialCell.Switch.On,
                minSpecial: MinSpecialCell.Value,
                minNumbers: MinNumbersCell.Value);
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
                        cell.TextLabel.Text = AppResources.RegeneratePassword;
                    }
                    else if(indexPath.Row == 1)
                    {
                        cell.TextLabel.Text = AppResources.CopyPassword;
                    }
                    return cell;
                }

                if(indexPath.Row == 0)
                {
                    return _controller.LengthCell;
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
                    return _controller.MinNumbersCell;
                }
                else if(indexPath.Row == 6)
                {
                    return _controller.MinSpecialCell;
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
                if(section == 0)
                {
                    return 0.00001f;
                }

                return UITableView.AutomaticDimension;
            }

            public override UIView GetViewForHeader(UITableView tableView, nint section)
            {
                if(section == 0)
                {
                    return new UIView(CGRect.Empty)
                    {
                        Hidden = true
                    };
                }

                return null;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                if(section == 1)
                {
                    return AppResources.Options;
                }

                return null;
            }

            public override string TitleForFooter(UITableView tableView, nint section)
            {
                if(section == 1)
                {
                    return AppResources.OptionDefaults;
                }

                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                if(indexPath.Section == 0)
                {
                    if(indexPath.Row == 0)
                    {
                        _controller._googleAnalyticsService.TrackExtensionEvent("RegeneratedPassword");
                        _controller.GeneratePassword();
                    }
                    else if(indexPath.Row == 1)
                    {
                        _controller._googleAnalyticsService.TrackExtensionEvent("CopiedGeneratedPassword");
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = _controller.PasswordLabel.Text;
                        var alert = Dialogs.CreateMessageAlert(AppResources.Copied);
                        _controller.PresentViewController(alert, true, () =>
                        {
                            _controller.DismissViewController(true, null);
                        });
                    }
                }

                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
            }

            public NSDate DateTimeToNSDate(DateTime date)
            {
                DateTime reference = TimeZone.CurrentTimeZone.ToLocalTime(
                    new DateTime(2001, 1, 1, 0, 0, 0));
                return NSDate.FromTimeIntervalSinceReferenceDate(
                    (date - reference).TotalSeconds);
            }
        }
    }
}
