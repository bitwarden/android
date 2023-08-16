namespace Bit.App.Pages
{
    public partial class AppearanceSettingsPage : BaseModalContentPage
    {
        public AppearanceSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as AppearanceSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
