namespace Bit.App.Pages
{
    public partial class SecuritySettingsPage : BaseContentPage
    {
        public SecuritySettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as SecuritySettingsPageViewModel;
            vm.Page = this;
        }
    }
}
