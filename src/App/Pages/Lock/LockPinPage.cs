using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Models.Page;
using Bit.App.Controls;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class LockPinPage : BaseLockPage
    {
        private readonly IAuthService _authService;
        private readonly IAppSettingsService _appSettingsService;
        private TapGestureRecognizer _tgr;
        private DateTime? _lastAction;

        public LockPinPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();

            Init();
        }

        public PinPageModel Model { get; set; } = new PinPageModel();
        public PinControl PinControl { get; set; }

        public void Init()
        {
            var instructionLabel = new Label
            {
                Text = AppResources.EnterPIN,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                HorizontalTextAlignment = TextAlignment.Center,
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            PinControl = new PinControl();
            PinControl.Label.SetBinding(Label.TextProperty, nameof(PinPageModel.LabelText));
            PinControl.Entry.SetBinding(Entry.TextProperty, nameof(PinPageModel.PIN));

            var logoutButton = new ExtendedButton
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                BackgroundColor = Color.Transparent,
                Uppercase = false
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 20,
                Children = { PinControl.Label, instructionLabel, logoutButton, PinControl.Entry }
            };

            _tgr = new TapGestureRecognizer();
            PinControl.Label.GestureRecognizers.Add(_tgr);
            instructionLabel.GestureRecognizers.Add(_tgr);

            Title = AppResources.VerifyPIN;
            Content = stackLayout;
            Content.GestureRecognizers.Add(_tgr);
            BindingContext = Model;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            PinControl.Entry.Focus();
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _tgr.Tapped += Tgr_Tapped;
            PinControl.OnPinEntered += PinEntered;
            PinControl.InitEvents();

            if(Device.RuntimePlatform == Device.Android)
            {
                Task.Run(async () =>
                {
                    for(int i = 0; i < 5; i++)
                    {
                        await Task.Delay(1000);
                        if(!PinControl.Entry.IsFocused)
                        {
                            Device.BeginInvokeOnMainThread(() => PinControl.Entry.Focus());
                        }
                        else
                        {
                            break;
                        }
                    }
                });
            }
            else
            {
                PinControl.Entry.Focus();
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _tgr.Tapped -= Tgr_Tapped;
            PinControl.OnPinEntered -= PinEntered;
            PinControl.Dispose();
        }

        protected async void PinEntered(object sender, EventArgs args)
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            if(Model.PIN == _authService.PIN)
            {
                _appSettingsService.Locked = false;
                _appSettingsService.FailedPinAttempts = 0;
                PinControl.Entry.Unfocus();
                if(Navigation.ModalStack.Count > 0)
                {
                    await Navigation.PopModalAsync();
                }
            }
            else
            {
                _appSettingsService.FailedPinAttempts++;
                if(_appSettingsService.FailedPinAttempts >= 5)
                {
                    PinControl.Entry.Unfocus();
                    AuthService.LogOut();
                    return;
                }

                await DisplayAlert(null, AppResources.InvalidPIN, AppResources.Ok);
                Model.PIN = string.Empty;
                PinControl.Entry.Focus();
            }
        }
    }
}
