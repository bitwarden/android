namespace Bit.App.Pages
{
    public partial class OtherSettingsPage : BaseModalContentPage
    {
        public OtherSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as OtherSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
