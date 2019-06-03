using Bit.App.Resources;
using System;
using System.Collections.Generic;
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
            if(Device.RuntimePlatform == Device.Android)
            {
                _vm.AndroidOptions = true;
                _themeDescriptionLabel.Text = string.Concat(_themeDescriptionLabel.Text, " ",
                    AppResources.RestartIsRequired);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        protected async override void OnDisappearing()
        {
            base.OnDisappearing();
            await _vm.UpdateAutofillBlacklistedUris();
        }

        private async void BlacklistedUrisEditor_Unfocused(object sender, FocusEventArgs e)
        {
            await _vm.UpdateAutofillBlacklistedUris();
        }
    }
}
