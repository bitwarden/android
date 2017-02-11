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

        public SettingsPinPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();

            Init();
        }

        public PinPageModel Model { get; set; } = new PinPageModel();
        public PinControl PinControl { get; set; }
        public EventHandler OnPinEntered;

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
            PinControl.OnPinEntered += PinEntered;
            PinControl.Label.SetBinding<PinPageModel>(Label.TextProperty, s => s.LabelText);
            PinControl.Entry.SetBinding<PinPageModel>(Entry.TextProperty, s => s.PIN);

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(30, 40),
                Spacing = 20,
                Children = { PinControl.Label, instructionLabel, PinControl.Entry }
            };

            var tgr = new TapGestureRecognizer();
            tgr.Tapped += Tgr_Tapped;
            PinControl.Label.GestureRecognizers.Add(tgr);
            instructionLabel.GestureRecognizers.Add(tgr);

            if(Device.OS == TargetPlatform.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
            }

            Title = AppResources.SetPIN;
            Content = stackLayout;
            Content.GestureRecognizers.Add(tgr);
            BindingContext = Model;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            PinControl.Entry.Focus();
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PinControl.Entry.FocusWithDelay();
        }

        protected void PinEntered(object sender, EventArgs args)
        {
            OnPinEntered.Invoke(this, null);
        }
    }
}
