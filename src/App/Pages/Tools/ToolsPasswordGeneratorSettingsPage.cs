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
        public StepperCell SpecialMinCell { get; set; }
        public StepperCell NumbersMinCell { get; set; }

        public void Init()
        {
            UppercaseCell = new ExtendedSwitchCell
            {
                Text = "A-Z",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, true)
            };

            LowercaseCell = new ExtendedSwitchCell
            {
                Text = "a-z",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, true)
            };

            SpecialCell = new ExtendedSwitchCell
            {
                Text = "!@#$%^&*",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, true)
            };

            NumbersCell = new ExtendedSwitchCell
            {
                Text = "0-9",
                On = _settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, true)
            };

            AvoidAmbiguousCell = new ExtendedSwitchCell
            {
                Text = AppResources.AvoidAmbiguousCharacters,
                On = !_settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, false)
            };

            NumbersMinCell = new StepperCell(AppResources.MinNumbers, 
                _settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1), 0, 5, 1);
            SpecialMinCell = new StepperCell(AppResources.MinSpecial,
                _settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1), 0, 5, 1);

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection(" ")
                    {
                        UppercaseCell,
                        LowercaseCell,
                        NumbersCell,
                        SpecialCell
                    },
                    new TableSection(" ")
                    {
                        NumbersMinCell,
                        SpecialMinCell
                    },
                    new TableSection(" ")
                    {
                        AvoidAmbiguousCell
                    }
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
            }

            Title = AppResources.Settings;
            Content = table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            SpecialCell.OnChanged += SpecialCell_OnChanged;
            AvoidAmbiguousCell.OnChanged += AvoidAmbiguousCell_OnChanged;
            UppercaseCell.OnChanged += UppercaseCell_OnChanged;
            LowercaseCell.OnChanged += LowercaseCell_OnChanged;
            NumbersCell.OnChanged += NumbersCell_OnChanged;
            NumbersMinCell.InitEvents();
            SpecialMinCell.InitEvents();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            SpecialCell.OnChanged -= SpecialCell_OnChanged;
            AvoidAmbiguousCell.OnChanged -= AvoidAmbiguousCell_OnChanged;
            UppercaseCell.OnChanged -= UppercaseCell_OnChanged;
            LowercaseCell.OnChanged -= LowercaseCell_OnChanged;
            NumbersCell.OnChanged -= NumbersCell_OnChanged;
            NumbersMinCell.Dispose();
            SpecialMinCell.Dispose();

            _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinNumbers, 
                Convert.ToInt32(NumbersMinCell.Stepper.Value));

            _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinSpecial,
                Convert.ToInt32(SpecialMinCell.Stepper.Value));
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
