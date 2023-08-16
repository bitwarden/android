namespace Bit.App.Pages
{
    public partial class SecuritySettingsPage : BaseModalContentPage
    {
        public SecuritySettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as SecuritySettingsPageViewModel;
            vm.Page = this;
        }
    }
}
