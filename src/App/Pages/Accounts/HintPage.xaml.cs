using System;

namespace Bit.App.Pages
{
    public partial class HintPage : BaseContentPage
    {
        private HintPageViewModel _vm;

        public HintPage()
        {
            InitializeComponent();
            _vm = BindingContext as HintPageViewModel;
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
