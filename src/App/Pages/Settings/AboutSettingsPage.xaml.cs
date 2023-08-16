namespace Bit.App.Pages
{
    public partial class AboutSettingsPage : BaseModalContentPage
    {
        public AboutSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as AboutSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
