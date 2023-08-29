using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SettingsPage : BaseContentPage
    {
        private readonly TabsPage _tabsPage;

        public SettingsPage(TabsPage tabsPage)
        {
            _tabsPage = tabsPage;
            InitializeComponent();
            var vm = BindingContext as SettingsPageViewModel;
            vm.Page = this;
        }

        protected override bool OnBackButtonPressed()
        {
            if (Device.RuntimePlatform == Device.Android && _tabsPage != null)
            {
                _tabsPage.ResetToVaultPage();
                return true;
            }
            return base.OnBackButtonPressed();
        }
    }
}
