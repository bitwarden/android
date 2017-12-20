using System;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class ToolsPasswordGeneratorPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly ISettings _settings;
        private readonly IDeviceActionService _clipboardService;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly Action<string> _passwordValueAction;
        private readonly bool _fromAutofill;

        public ToolsPasswordGeneratorPage(Action<string> passwordValueAction = null, bool fromAutofill = false)
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _passwordGenerationService = Resolver.Resolve<IPasswordGenerationService>();
            _settings = Resolver.Resolve<ISettings>();
            _clipboardService = Resolver.Resolve<IDeviceActionService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _passwordValueAction = passwordValueAction;
            _fromAutofill = fromAutofill;

            Init();
        }

        public PasswordGeneratorPageModel Model { get; private set; } = new PasswordGeneratorPageModel();
        public Label Password { get; private set; }
        public SliderViewCell SliderCell { get; private set; }
        public TapGestureRecognizer Tgr { get; set; }
        public ExtendedTextCell RegenerateCell { get; set; }
        public ExtendedTextCell CopyCell { get; set; }
        public ExtendedSwitchCell UppercaseCell { get; set; }
        public ExtendedSwitchCell LowercaseCell { get; set; }
        public ExtendedSwitchCell SpecialCell { get; set; }
        public ExtendedSwitchCell NumbersCell { get; set; }
        public ExtendedSwitchCell AvoidAmbiguousCell { get; set; }
        public StepperCell SpecialMinCell { get; set; }
        public StepperCell NumbersMinCell { get; set; }

        public void Init()
        {
            Password = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                Margin = new Thickness(15, 40, 15, 40),
                HorizontalTextAlignment = TextAlignment.Center,
                FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier"),
                LineBreakMode = LineBreakMode.TailTruncation,
                VerticalOptions = LayoutOptions.Start,
                TextColor = Color.Black
            };

            Tgr = new TapGestureRecognizer();
            Password.GestureRecognizers.Add(Tgr);
            Password.SetBinding(Label.TextProperty, nameof(PasswordGeneratorPageModel.Password));

            SliderCell = new SliderViewCell(this, _passwordGenerationService, _settings);

            var buttonColor = Color.FromHex("3c8dbc");
            RegenerateCell = new ExtendedTextCell { Text = AppResources.RegeneratePassword, TextColor = buttonColor };
            CopyCell = new ExtendedTextCell { Text = AppResources.CopyPassword, TextColor = buttonColor };

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
                _settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1), 0, 5, 1, () =>
                {
                    _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinNumbers,
                        Convert.ToInt32(NumbersMinCell.Stepper.Value));
                    Model.Password = _passwordGenerationService.GeneratePassword();
                });

            SpecialMinCell = new StepperCell(AppResources.MinSpecial,
                _settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1), 0, 5, 1, () =>
                {
                    _settings.AddOrUpdateValue(Constants.PasswordGeneratorMinSpecial,
                        Convert.ToInt32(SpecialMinCell.Stepper.Value));
                    Model.Password = _passwordGenerationService.GeneratePassword();
                });

            var table = new ExtendedTableView
            {
                VerticalOptions = LayoutOptions.Start,
                EnableScrolling = false,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                NoHeader = true,
                Root = new TableRoot
                {
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        RegenerateCell,
                        CopyCell
                    },
                    new TableSection(AppResources.Options)
                    {
                        SliderCell,
                        UppercaseCell,
                        LowercaseCell,
                        NumbersCell,
                        SpecialCell
                    },
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        NumbersMinCell,
                        SpecialMinCell
                    },
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        AvoidAmbiguousCell
                    }
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;

                if(_passwordValueAction != null)
                {
                    ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
                }
            }

            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Children = { Password, table },
                VerticalOptions = LayoutOptions.FillAndExpand,
                Spacing = 0
            };

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical,
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            if(_passwordValueAction != null)
            {
                var selectToolBarItem = new ToolbarItem(AppResources.Select, Helpers.ToolbarImage("ion_chevron_right.png"), async () =>
                {
                    if(_fromAutofill)
                    {
                        _googleAnalyticsService.TrackExtensionEvent("SelectedGeneratedPassword");
                    }
                    else
                    {
                        _googleAnalyticsService.TrackAppEvent("SelectedGeneratedPassword");
                    }

                    _passwordValueAction(Password.Text);
                    await Navigation.PopForDeviceAsync();
                }, ToolbarItemOrder.Default, 0);

                ToolbarItems.Add(selectToolBarItem);
            }

            Title = AppResources.PasswordGenerator;
            Content = scrollView;
            BindingContext = Model;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            CopyPassword();
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            Tgr.Tapped += Tgr_Tapped;
            RegenerateCell.Tapped += RegenerateCell_Tapped;
            CopyCell.Tapped += CopyCell_Tapped;
            SliderCell.InitEvents();
            SpecialCell.OnChanged += SpecialCell_OnChanged;
            AvoidAmbiguousCell.OnChanged += AvoidAmbiguousCell_OnChanged;
            UppercaseCell.OnChanged += UppercaseCell_OnChanged;
            LowercaseCell.OnChanged += LowercaseCell_OnChanged;
            NumbersCell.OnChanged += NumbersCell_OnChanged;
            NumbersMinCell.InitEvents();
            SpecialMinCell.InitEvents();

            if(_fromAutofill)
            {
                _googleAnalyticsService.TrackExtensionEvent("GeneratedPassword");
            }
            else
            {
                _googleAnalyticsService.TrackAppEvent("GeneratedPassword");
            }
            Model.Password = _passwordGenerationService.GeneratePassword();
            Model.Length = _settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10).ToString();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            Tgr.Tapped -= Tgr_Tapped;
            RegenerateCell.Tapped -= RegenerateCell_Tapped;
            SpecialCell.OnChanged -= SpecialCell_OnChanged;
            AvoidAmbiguousCell.OnChanged -= AvoidAmbiguousCell_OnChanged;
            UppercaseCell.OnChanged -= UppercaseCell_OnChanged;
            LowercaseCell.OnChanged -= LowercaseCell_OnChanged;
            NumbersCell.OnChanged -= NumbersCell_OnChanged;
            NumbersMinCell.Dispose();
            SpecialMinCell.Dispose();
            CopyCell.Tapped -= CopyCell_Tapped;
            SliderCell.Dispose();
        }

        private void RegenerateCell_Tapped(object sender, EventArgs e)
        {
            Model.Password = _passwordGenerationService.GeneratePassword();
            if(_fromAutofill)
            {
                _googleAnalyticsService.TrackExtensionEvent("RegeneratedPassword");
            }
            else
            {
                _googleAnalyticsService.TrackAppEvent("RegeneratedPassword");
            }
        }

        private void CopyCell_Tapped(object sender, EventArgs e)
        {
            CopyPassword();
        }

        private void CopyPassword()
        {
            if(_fromAutofill)
            {
                _googleAnalyticsService.TrackExtensionEvent("CopiedGeneratedPassword");
            }
            else
            {
                _googleAnalyticsService.TrackAppEvent("CopiedGeneratedPassword");
            }
            _clipboardService.CopyToClipboard(Password.Text);
            _userDialogs.Toast(string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
        }

        private void AvoidAmbiguousCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorAmbiguous, !AvoidAmbiguousCell.On);
            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private void NumbersCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorNumbers, NumbersCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }

            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private void SpecialCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorSpecial, SpecialCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }

            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private void LowercaseCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, LowercaseCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorUppercase, true);
                UppercaseCell.On = true;
            }

            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private void UppercaseCell_OnChanged(object sender, ToggledEventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.PasswordGeneratorUppercase, UppercaseCell.On);

            if(InvalidState())
            {
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLowercase, true);
                LowercaseCell.On = true;
            }

            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private bool InvalidState()
        {
            return !LowercaseCell.On && !UppercaseCell.On && !NumbersCell.On && !SpecialCell.On;
        }


        // TODO: move to standalone reusable control
        public class SliderViewCell : ExtendedViewCell, IDisposable
        {
            private readonly ToolsPasswordGeneratorPage _page;
            private readonly IPasswordGenerationService _passwordGenerationService;
            private readonly ISettings _settings;

            public Label Value { get; set; }
            public Slider LengthSlider { get; set; }

            public SliderViewCell(
                ToolsPasswordGeneratorPage page,
                IPasswordGenerationService passwordGenerationService,
                ISettings settings)
            {
                _page = page;
                _passwordGenerationService = passwordGenerationService;
                _settings = settings;

                var label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    Text = AppResources.Length,
                    HorizontalOptions = LayoutOptions.Start,
                    VerticalOptions = LayoutOptions.CenterAndExpand
                };

                LengthSlider = new Slider(5, 64, _settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10))
                {
                    HorizontalOptions = LayoutOptions.FillAndExpand,
                    VerticalOptions = LayoutOptions.CenterAndExpand
                };

                Value = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.CenterAndExpand,
                    Style = (Style)Application.Current.Resources["text-muted"]
                };

                Value.SetBinding(Label.TextProperty, nameof(PasswordGeneratorPageModel.Length));

                var stackLayout = new StackLayout
                {
                    Orientation = StackOrientation.Horizontal,
                    Spacing = 15,
                    Children = { label, LengthSlider, Value },
                    Padding = Helpers.OnPlatform(
                        iOS: new Thickness(15, 8),
                        Android: new Thickness(16, 10),
                        Windows: new Thickness(15, 8))
                };

                stackLayout.AdjustPaddingForDevice();
                if(Device.RuntimePlatform == Device.Android)
                {
                    label.TextColor = Color.Black;
                }

                View = stackLayout;
            }

            private void Slider_ValueChanged(object sender, ValueChangedEventArgs e)
            {
                var length = Convert.ToInt32(LengthSlider.Value);
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLength, length);
                _page.Model.Length = length.ToString();
                _page.Model.Password = _passwordGenerationService.GeneratePassword();
            }

            public void InitEvents()
            {
                LengthSlider.ValueChanged += Slider_ValueChanged;
            }

            public void Dispose()
            {
                LengthSlider.ValueChanged -= Slider_ValueChanged;
            }
        }
    }
}
