using System;

namespace Bit.App.Pages
{
    public partial class GeneratorHistoryPage : BaseContentPage
    {
        private GeneratorHistoryPageViewModel _vm;

        public GeneratorHistoryPage()
        {
            InitializeComponent();
            SetActivityIndicator();
            _vm = BindingContext as GeneratorHistoryPageViewModel;
            _vm.Page = this;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, true, async () => {
                await _vm.InitAsync();
            });
        }

        private async void Clear_Clicked(object sender, EventArgs e)
        {
            await _vm.ClearAsync();
        }
    }
}
