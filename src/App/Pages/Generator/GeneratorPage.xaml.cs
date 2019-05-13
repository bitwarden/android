using System;

namespace Bit.App.Pages
{
    public partial class GeneratorPage : BaseContentPage
    {
        private GeneratorPageViewModel _vm;

        public GeneratorPage()
        {
            InitializeComponent();
            _vm = BindingContext as GeneratorPageViewModel;
            _vm.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private async void Regenerate_Clicked(object sender, EventArgs e)
        {
            await _vm.RegenerateAsync();
        }

        private async void Copy_Clicked(object sender, EventArgs e)
        {
            await _vm.CopyAsync();
        }

        private void Select_Clicked(object sender, EventArgs e)
        {

        }

        private void History_Clicked(object sender, EventArgs e)
        {

        }
    }
}
