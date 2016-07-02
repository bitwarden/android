using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class ToolsPasswordGeneratorSettingsPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;

        public ToolsPasswordGeneratorSettingsPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public ExtendedSwitchCell UppercaseCell { get; set; }
        public ExtendedSwitchCell LowercaseCell { get; set; }
        public ExtendedSwitchCell SpecialCell { get; set; }
        public ExtendedSwitchCell NumbersCell { get; set; }
        public ExtendedSwitchCell AvoidAmbiguousCell { get; set; }
        public EntryCell SpecialMinCell { get; set; }
        public EntryCell NumbersMinCell { get; set; }

        public void Init()
        {
            UppercaseCell = new ExtendedSwitchCell
            {
                Text = "A-Z",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, true)
            };
            UppercaseCell.OnChanged += UppercaseCell_OnChanged;

            LowercaseCell = new ExtendedSwitchCell
            {
                Text = "a-z",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, true)
            };
            LowercaseCell.OnChanged += LowercaseCell_OnChanged;

            SpecialCell = new ExtendedSwitchCell
            {
                Text = "!@#$%^&*",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, true)
            };
            SpecialCell.OnChanged += SpecialCell_OnChanged;

            NumbersCell = new ExtendedSwitchCell
            {
                Text = "0-9",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, true)
            };
            NumbersCell.OnChanged += NumbersCell_OnChanged;

            AvoidAmbiguousCell = new ExtendedSwitchCell
            {
                Text = "Avoid Ambiguous Characters",
                On = !_settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, false)
            };
            AvoidAmbiguousCell.OnChanged += AvoidAmbiguousCell_OnChanged; ;

            NumbersMinCell = new EntryCell
            {
                Label = "Minimum Numbers",
                Text = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1).ToString(),
                Keyboard = Keyboard.Numeric
            };

            SpecialMinCell = new EntryCell
            {
                Label = "Minimum Special",
                Text = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1).ToString(),
                Keyboard = Keyboard.Numeric
            };

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Menu,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        UppercaseCell,
                        LowercaseCell,
                        NumbersCell,
                        SpecialCell
                    },
                    new TableSection
                    {
                        NumbersMinCell,
                        SpecialMinCell
                    },
                    new TableSection
                    {
                        AvoidAmbiguousCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
            }

            Title = "Settings";
            Content = table;
        }

        protected override void OnDisappearing()
        {
            int minNumber;
            if(int.TryParse(NumbersMinCell.Text, out minNumber))
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinNumbers, minNumber);
            }

            int minSpecial;
            if(int.TryParse(SpecialMinCell.Text, out minSpecial))
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinSpecial, minSpecial);
            }
        }

        private void AvoidAmbiguousCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorAmbiguous, !AvoidAmbiguousCell.On);
        }

        private void NumbersCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorNumbers, NumbersCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }
        }

        private void SpecialCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorSpecial, SpecialCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }
        }

        private void LowercaseCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, LowercaseCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorUppercase, true);
                UppercaseCell.On = true;
            }
        }

        private void UppercaseCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorUppercase, UppercaseCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }
        }

        private bool InvalidState()
        {
            return !LowercaseCell.On && !UppercaseCell.On && !NumbersCell.On && !SpecialCell.On;
        }
    }
}
