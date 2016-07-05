using System;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Page;
using Bit.App.Resources;
using Plugin.Settings.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class ToolsPasswordGeneratorPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly ISettings _settings;
        private readonly IClipboardService _clipboardService;

        public ToolsPasswordGeneratorPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _passwordGenerationService = Resolver.Resolve<IPasswordGenerationService>();
            _settings = Resolver.Resolve<ISettings>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }

        public PasswordGeneratorPageModel Model { get; private set; } = new PasswordGeneratorPageModel();
        public Label Password { get; private set; }
        public SliderViewCell SliderCell { get; private set; }

        public void Init()
        {
            Password = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                Margin = new Thickness(15, 40, 15, 0),
                HorizontalTextAlignment = TextAlignment.Center,
                FontFamily = "Courier",
                LineBreakMode = LineBreakMode.TailTruncation,
                VerticalOptions = LayoutOptions.Start
            };

            var tgr = new TapGestureRecognizer();
            tgr.Tapped += Tgr_Tapped;
            Password.GestureRecognizers.Add(tgr);
            Password.SetBinding<PasswordGeneratorPageModel>(Label.TextProperty, m => m.Password);

            SliderCell = new SliderViewCell(this, _passwordGenerationService, _settings);
            var settingsCell = new ExtendedTextCell { Text = "More Settings", ShowDisclousure = true };
            settingsCell.Tapped += SettingsCell_Tapped;

            var buttonColor = Color.FromHex("3c8dbc");
            var regenerateCell = new ExtendedTextCell { Text = "Regenerate Password", TextColor = buttonColor };
            regenerateCell.Tapped += RegenerateCell_Tapped; ;
            var copyCell = new ExtendedTextCell { Text = "Copy Password", TextColor = buttonColor };
            copyCell.Tapped += CopyCell_Tapped;
            var saveCell = new ExtendedTextCell { Text = "Save Password", TextColor = buttonColor };
            saveCell.Tapped += SaveCell_Tapped;

            var table = new ExtendedTableView
            {
                EnableScrolling = false,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        SliderCell,
                        settingsCell
                    },
                    new TableSection
                    {
                        regenerateCell,
                        copyCell
                    },
                    new TableSection
                    {
                        saveCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Close"));
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

            Title = "Generate Password";
            Content = scrollView;
            BindingContext = Model;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            CopyPassword();
        }

        protected override void OnAppearing()
        {
            Model.Password = _passwordGenerationService.GeneratePassword();
            Model.Length = _settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10).ToString();
            base.OnAppearing();
        }

        private void RegenerateCell_Tapped(object sender, EventArgs e)
        {
            Model.Password = _passwordGenerationService.GeneratePassword();
        }

        private void SaveCell_Tapped(object sender, EventArgs e)
        {

        }

        private void CopyCell_Tapped(object sender, EventArgs e)
        {
            CopyPassword();
        }

        private void SettingsCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new ToolsPasswordGeneratorSettingsPage());
        }

        private void CopyPassword()
        {
            _clipboardService.CopyToClipboard(Password.Text);
            _userDialogs.SuccessToast(string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
        }

        public class SliderViewCell : ExtendedViewCell
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
                    Text = "Length",
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
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.CenterAndExpand,
                    Style = (Style)Application.Current.Resources["text-muted"]
                };

                Value.SetBinding<PasswordGeneratorPageModel>(Label.TextProperty, m => m.Length);

                LengthSlider.ValueChanged += Slider_ValueChanged;

                var stackLayout = new StackLayout
                {
                    Orientation = StackOrientation.Horizontal,
                    Spacing = 15,
                    Children = { label, LengthSlider, Value },
                    Padding = new Thickness(15)
                };

                View = stackLayout;
            }

            private void Slider_ValueChanged(object sender, ValueChangedEventArgs e)
            {
                var length = Convert.ToInt32(LengthSlider.Value);
                _settings.AddOrUpdateValue(Constants.PasswordGeneratorLength, length);
                _page.Model.Length = length.ToString();
                _page.Model.Password = _passwordGenerationService.GeneratePassword();
            }
        }
    }
}
