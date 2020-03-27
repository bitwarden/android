using System;

namespace Bit.App.Pages
{
    public partial class ExtensionPage : BaseContentPage
    {
        private readonly ExtensionPageViewModel _vm;

        public ExtensionPage()
        {
            InitializeComponent();
            _vm = BindingContext as ExtensionPageViewModel;
            _vm.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private void Show_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.ShowExtension();
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PopModalAsync();
            }
        }
    }
}
