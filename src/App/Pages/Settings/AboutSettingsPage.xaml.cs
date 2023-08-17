namespace Bit.App.Pages
{
    public partial class AboutSettingsPage : BaseContentPage
    {
        public AboutSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as AboutSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
