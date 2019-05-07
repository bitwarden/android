using System;

namespace Bit.App.Pages
{
    public partial class EnvironmentPage : BaseContentPage
    {
        private EnvironmentPageViewModel _vm;

        public EnvironmentPage()
        {
            InitializeComponent();
            _vm = BindingContext as EnvironmentPageViewModel;
            _vm.Page = this;
        }

        private async void Submit_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }
    }
}
