namespace Bit.App.Pages
{
    public partial class AutofillSettingsPage : BaseContentPage
    {
        public AutofillSettingsPage()
        {
            InitializeComponent();
            var vm = BindingContext as AutofillSettingsPageViewModel;
            vm.Page = this;
        }
    }
}
