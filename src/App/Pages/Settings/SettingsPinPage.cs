using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Page;
using Bit.App.Controls;

namespace Bit.App.Pages
{
    public class SettingsPinPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private Action<SettingsPinPage> _pinEnteredAction;

        public SettingsPinPage(Action<SettingsPinPage> pinEnteredAction)
        {
            _pinEnteredAction = pinEnteredAction;
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public PinPageModel Model { get; set; } = new PinPageModel();
        public PinControl PinControl { get; set; }
        public TapGestureRecognizer Tgr { get; set; }

        public void Init()
        {
            var instructionLabel = new Label
            {
                Text = AppResources.SetPINDirection,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                HorizontalTextAlignment = TextAlignment.Center,
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            PinControl = new PinControl();
            PinControl.Label.SetBinding(Label.TextProperty, nameof(PinPageModel.LabelText));
            PinControl.Entry.SetBinding(Entry.TextProperty, nameof(PinPageModel.PIN));

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 20,
                Children = { PinControl.Label, instructionLabel, PinControl.Entry }
            };

            Tgr = new TapGestureRecognizer();
            PinControl.Label.GestureRecognizers.Add(Tgr);
            instructionLabel.GestureRecognizers.Add(Tgr);

            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.UWP)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
            }

            Title = AppResources.SetPIN;
            Content = stackLayout;
            Content.GestureRecognizers.Add(Tgr);
            BindingContext = Model;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            Tgr.Tapped += Tgr_Tapped;
            PinControl.OnPinEntered += PinEntered;
            PinControl.InitEvents();
            PinControl.Entry.FocusWithDelay();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            PinControl.Dispose();
            Tgr.Tapped -= Tgr_Tapped;
            PinControl.OnPinEntered -= PinEntered;
        }

        protected void PinEntered(object sender, EventArgs args)
        {
            _pinEnteredAction?.Invoke(this);
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            PinControl.Entry.Focus();
        }
    }
}
