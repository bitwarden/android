using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AccessibilityServicePage : BaseContentPage
    {
        private readonly AccessibilityServicePageViewModel _vm;
        private readonly SettingsPage _settingsPage;
        private DateTime? _timerStarted = null;
        private TimeSpan _timerMaxLength = TimeSpan.FromMinutes(5);

        public AccessibilityServicePage(SettingsPage settingsPage)
        {
            InitializeComponent();
            _vm = BindingContext as AccessibilityServicePageViewModel;
            _vm.Page = this;
            _settingsPage = settingsPage;
        }

        protected override void OnAppearing()
        {
            _vm.UpdateEnabled();
            _vm.UpdatePermitted();
            _timerStarted = DateTime.UtcNow;
            Device.StartTimer(new TimeSpan(0, 0, 3), () =>
            {
                if (_timerStarted == null || (DateTime.UtcNow - _timerStarted) > _timerMaxLength)
                {
                    return false;
                }
                _vm.UpdateEnabled();
                _vm.UpdatePermitted();
                return true;
            });
            base.OnAppearing();
        }

        protected override void OnDisappearing()
        {
            _timerStarted = null;
            _settingsPage.BuildList();
            base.OnDisappearing();
        }

        private void Settings_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.OpenSettings();
            }
        }

        private void OverlayPermissionSettings_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.OpenOverlayPermissionSettings();
            }
        }
    }
}
