using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public partial class OptionsPage : BaseContentPage
    {
        private readonly IAutofillHandler _autofillHandler;
        private readonly OptionsPageViewModel _vm;

        public OptionsPage()
        {
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            InitializeComponent();
            _vm = BindingContext as OptionsPageViewModel;
            _vm.Page = this;
            _themePicker.ItemDisplayBinding = new Binding("Value");
            _autoDarkThemePicker.ItemDisplayBinding = new Binding("Value");
            _uriMatchPicker.ItemDisplayBinding = new Binding("Value");
            _clearClipboardPicker.ItemDisplayBinding = new Binding("Value");
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
                _vm.ShowAndroidAutofillSettings = _autofillHandler.SupportsAutofillService();
            }
            else
            {
                _themePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _autoDarkThemePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _uriMatchPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _clearClipboardPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _languagePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
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
            await _vm.UpdateAutofillBlockedUris();
        }

        private async void AutofillBlockedUrisEditor_Unfocused(object sender, FocusEventArgs e)
        {
            await _vm.UpdateAutofillBlockedUris();
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
