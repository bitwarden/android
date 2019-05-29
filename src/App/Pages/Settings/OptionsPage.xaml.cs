using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class OptionsPage : BaseContentPage
    {
        private readonly OptionsPageViewModel _vm;

        public OptionsPage()
        {
            InitializeComponent();
            _vm = BindingContext as OptionsPageViewModel;
            _vm.Page = this;
            _themePicker.ItemDisplayBinding = new Binding("Value");
            _uriMatchPicker.ItemDisplayBinding = new Binding("Value");
            _clearClipboardPicker.ItemDisplayBinding = new Binding("Value");
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }
    }
}
