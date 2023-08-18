namespace Bit.App.Pages
{
    public partial class VaultSettingsPage : BaseContentPage
    {
        public VaultSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as VaultSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
