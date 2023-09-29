using System;
using System.Linq;
using Bit.iOS.Core.Views;
using Bit.iOS.Core.Models;
using Foundation;
using UIKit;
using CoreGraphics;
using Bit.iOS.Core.Utilities;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using System.Collections.Generic;

namespace Bit.iOS.Core.Controllers
{
    public abstract class PasswordGeneratorViewController : ExtendedUIViewController
    {
        private IPasswordGenerationService _passwordGenerationService;
        private string _passType;

        public PasswordGeneratorViewController(IntPtr handle)
            : base(handle)
        { }

        public UITableViewController OptionsTableViewController { get; set; }
        public PickerTableViewCell TypePickerCell { get; set; } = new PickerTableViewCell(
            AppResources.Type);

        public SwitchTableViewCell UppercaseCell { get; set; } = new SwitchTableViewCell("A-Z");
        public SwitchTableViewCell LowercaseCell { get; set; } = new SwitchTableViewCell("a-z");
        public SwitchTableViewCell NumbersCell { get; set; } = new SwitchTableViewCell("0-9");
        public SwitchTableViewCell SpecialCell { get; set; } = new SwitchTableViewCell("!@#$%^&*");
        public StepperTableViewCell MinNumbersCell { get; set; } = new StepperTableViewCell(
            AppResources.MinNumbers, 1, 0, 5, 1);
        public StepperTableViewCell MinSpecialCell { get; set; } = new StepperTableViewCell(
            AppResources.MinSpecial, 1, 0, 5, 1);
        public SliderTableViewCell LengthCell { get; set; } = new SliderTableViewCell(
            AppResources.Length, 10, 5, 64);
        public SwitchTableViewCell AmbiguousCell { get; set; } = new SwitchTableViewCell(
            AppResources.AvoidAmbiguousCharacters);

        public StepperTableViewCell NumWordsCell { get; set; } = new StepperTableViewCell(
            AppResources.NumberOfWords, 3, 3, 20, 1);
        public FormEntryTableViewCell WordSeparatorCell { get; set; } = new FormEntryTableViewCell(
            AppResources.WordSeparator, leadingConstant: 20f);
        public SwitchTableViewCell CapitalizeCell { get; set; } = new SwitchTableViewCell(
            AppResources.Capitalize);
        public SwitchTableViewCell IncludeNumberCell { get; set; } = new SwitchTableViewCell(
            AppResources.IncludeNumber);

        public List<string> TypeOptions { get; set; } = new List<string> {
            AppResources.Password, AppResources.Passphrase };
        public PasswordGenerationOptions PasswordOptions { get; set; }
        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSelectBarButton { get; }
        public abstract UILabel BasePasswordLabel { get; }

        public async override void ViewDidLoad()
        {
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");

            BaseNavItem.Title = AppResources.PasswordGenerator;
            BaseCancelButton.Title = AppResources.Cancel;
            BaseSelectBarButton.Title = AppResources.Select;

            var descriptor = UIFontDescriptor.PreferredBody;
            BasePasswordLabel.Font = UIFont.FromName("Menlo-Regular", descriptor.PointSize * 1.3f);
            BasePasswordLabel.LineBreakMode = UILineBreakMode.TailTruncation;
            BasePasswordLabel.Lines = 0;
            BasePasswordLabel.AdjustsFontSizeToFitWidth = false;
            BasePasswordLabel.TextColor = ThemeHelpers.TextColor;

            var controller = ChildViewControllers.LastOrDefault();
            if (controller != null)
            {
                OptionsTableViewController = controller as UITableViewController;
            }

            if (OptionsTableViewController != null)
            {
                OptionsTableViewController.TableView.RowHeight = UITableView.AutomaticDimension;
                OptionsTableViewController.TableView.EstimatedRowHeight = 70;
                OptionsTableViewController.TableView.Source = new TableSource(this);
                OptionsTableViewController.TableView.AllowsSelection = true;
                OptionsTableViewController.View.BackgroundColor = ThemeHelpers.BackgroundColor;
                OptionsTableViewController.TableView.SeparatorColor = ThemeHelpers.SeparatorColor;
            }

            TypePickerCell.Items = TypeOptions;
            TypePickerCell.ValueChanged += Type_ValueChanged;
            SetPassType();

            var (options, enforcedPolicyOptions) = await _passwordGenerationService.GetOptionsAsync();
            UppercaseCell.Switch.On = options.Uppercase.GetValueOrDefault();
            LowercaseCell.Switch.On = options.Lowercase.GetValueOrDefault(true);
            SpecialCell.Switch.On = options.Special.GetValueOrDefault();
            NumbersCell.Switch.On = options.Number.GetValueOrDefault();
            MinNumbersCell.Value = options.MinNumber.GetValueOrDefault(1);
            MinSpecialCell.Value = options.MinSpecial.GetValueOrDefault(1);
            LengthCell.Value = options.Length.GetValueOrDefault(14);
            AmbiguousCell.Switch.On = !options.AllowAmbiguousChar.GetValueOrDefault();

            NumWordsCell.Value = options.NumWords.GetValueOrDefault(3);
            WordSeparatorCell.TextField.Text = options.WordSeparator ?? "";
            CapitalizeCell.Switch.On = options.Capitalize.GetValueOrDefault();
            IncludeNumberCell.Switch.On = options.IncludeNumber.GetValueOrDefault();

            UppercaseCell.ValueChanged += Options_ValueChanged;
            LowercaseCell.ValueChanged += Options_ValueChanged;
            NumbersCell.ValueChanged += Options_ValueChanged;
            SpecialCell.ValueChanged += Options_ValueChanged;
            MinNumbersCell.ValueChanged += Options_ValueChanged;
            MinSpecialCell.ValueChanged += Options_ValueChanged;
            LengthCell.ValueChanged += Options_ValueChanged;
            AmbiguousCell.ValueChanged += Options_ValueChanged;

            NumWordsCell.ValueChanged += Options_ValueChanged;
            WordSeparatorCell.ValueChanged += Options_ValueChanged;
            CapitalizeCell.ValueChanged += Options_ValueChanged;
            IncludeNumberCell.ValueChanged += Options_ValueChanged;

            // Adjust based on context password options
            if (PasswordOptions != null)
            {
                if (PasswordOptions.RequireDigits)
                {
                    NumbersCell.Switch.On = true;
                    NumbersCell.Switch.Enabled = false;

                    if (MinNumbersCell.Value < 1)
                    {
                        MinNumbersCell.Value = 1;
                    }

                    MinNumbersCell.Stepper.MinimumValue = 1;
                }

                if (PasswordOptions.RequireSymbols)
                {
                    SpecialCell.Switch.On = true;
                    SpecialCell.Switch.Enabled = false;

                    if (MinSpecialCell.Value < 1)
                    {
                        MinSpecialCell.Value = 1;
                    }

                    MinSpecialCell.Stepper.MinimumValue = 1;
                }

                if (PasswordOptions.MinLength < PasswordOptions.MaxLength)
                {
                    if (PasswordOptions.MinLength > 0 && PasswordOptions.MinLength > LengthCell.Slider.MinValue)
                    {
                        if (LengthCell.Value < PasswordOptions.MinLength)
                        {
                            LengthCell.Slider.Value = PasswordOptions.MinLength;
                        }

                        LengthCell.Slider.MinValue = PasswordOptions.MinLength;
                    }

                    if (PasswordOptions.MaxLength > 5 && PasswordOptions.MaxLength < LengthCell.Slider.MaxValue)
                    {
                        if (LengthCell.Value > PasswordOptions.MaxLength)
                        {
                            LengthCell.Slider.Value = PasswordOptions.MaxLength;
                        }

                        LengthCell.Slider.MaxValue = PasswordOptions.MaxLength;
                    }
                }
            }

            var task = GeneratePasswordAsync();
            base.ViewDidLoad();
        }

        private void Options_ValueChanged(object sender, EventArgs e)
        {
            if (InvalidState())
            {
                LowercaseCell.Switch.On = true;
            }
            var task = GeneratePasswordAsync();
        }

        private void Type_ValueChanged(object sender, EventArgs e)
        {
            SetPassType();
            OptionsTableViewController.TableView.ReloadData();
            var task = GeneratePasswordAsync();
        }

        private void SetPassType()
        {
            _passType = TypePickerCell.SelectedIndex == 1 ? "passphrase" : "password";
        }

        private bool InvalidState()
        {
            return !LowercaseCell.Switch.On && !UppercaseCell.Switch.On && !NumbersCell.Switch.On &&
                !SpecialCell.Switch.On;
        }

        private async Task GeneratePasswordAsync()
        {
            BasePasswordLabel.Text = await _passwordGenerationService.GeneratePasswordAsync(
                new Bit.Core.Models.Domain.PasswordGenerationOptions
                {
                    Type = _passType,
                    Length = LengthCell.Value,
                    Uppercase = UppercaseCell.Switch.On,
                    Lowercase = LowercaseCell.Switch.On,
                    Number = NumbersCell.Switch.On,
                    Special = SpecialCell.Switch.On,
                    MinSpecial = MinSpecialCell.Value,
                    MinNumber = MinNumbersCell.Value,
                    AllowAmbiguousChar = !AmbiguousCell.Switch.On,
                    NumWords = NumWordsCell.Value,
                    WordSeparator = WordSeparatorCell.TextField.Text,
                    Capitalize = CapitalizeCell.Switch.On,
                    IncludeNumber = IncludeNumberCell.Switch.On
                });
        }

        public class TableSource : ExtendedUITableViewSource
        {
            private PasswordGeneratorViewController _controller;

            public TableSource(PasswordGeneratorViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if (indexPath.Section == 0)
                {
                    var cell = new ExtendedUITableViewCell();
                    cell.TextLabel.TextColor = ThemeHelpers.PrimaryColor;
                    if (indexPath.Row == 0)
                    {
                        cell.TextLabel.Text = AppResources.RegeneratePassword;
                    }
                    else if (indexPath.Row == 1)
                    {
                        cell.TextLabel.Text = AppResources.CopyPassword;
                    }
                    return cell;
                }

                if (indexPath.Row == 0)
                {
                    return _controller.TypePickerCell;
                }

                if (_controller._passType == "password")
                {

                    if (indexPath.Row == 1)
                    {
                        return _controller.LengthCell;
                    }
                    else if (indexPath.Row == 2)
                    {
                        return _controller.UppercaseCell;
                    }
                    else if (indexPath.Row == 3)
                    {
                        return _controller.LowercaseCell;
                    }
                    else if (indexPath.Row == 4)
                    {
                        return _controller.NumbersCell;
                    }
                    else if (indexPath.Row == 5)
                    {
                        return _controller.SpecialCell;
                    }
                    else if (indexPath.Row == 6)
                    {
                        return _controller.MinNumbersCell;
                    }
                    else if (indexPath.Row == 7)
                    {
                        return _controller.MinSpecialCell;
                    } else if (indexPath.Row == 8)
                    {
                        return _controller.AmbiguousCell;
                    }    
                }
                else
                {
                    if (indexPath.Row == 1)
                    {
                        return _controller.NumWordsCell;
                    }
                    else if (indexPath.Row == 2)
                    {
                        return _controller.WordSeparatorCell;
                    }
                    else if (indexPath.Row == 3)
                    {
                        return _controller.CapitalizeCell;
                    }
                    else if (indexPath.Row == 4)
                    {
                        return _controller.IncludeNumberCell;
                    }
                }

                return new ExtendedUITableViewCell();
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
                if (section == 0)
                {
                    return 2;
                }

                if (_controller._passType == "password")
                {
                    return 9;
                }
                else
                {
                    return 5;
                }
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                if (section == 0)
                {
                    return 0.00001f;
                }

                return UITableView.AutomaticDimension;
            }

            public override UIView GetViewForHeader(UITableView tableView, nint section)
            {
                if (section == 0)
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
                if (section == 1)
                {
                    return AppResources.Options;
                }

                return null;
            }

            public override string TitleForFooter(UITableView tableView, nint section)
            {
                if (section == 1)
                {
                    return AppResources.OptionDefaults;
                }

                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                if (indexPath.Section == 0)
                {
                    if (indexPath.Row == 0)
                    {
                        var task = _controller.GeneratePasswordAsync();
                    }
                    else if (indexPath.Row == 1)
                    {
                        UIPasteboard clipboard = UIPasteboard.General;
                        clipboard.String = _controller.BasePasswordLabel.Text;
                        var alert = Dialogs.CreateMessageAlert(
                            string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
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
