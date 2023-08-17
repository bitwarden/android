using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public partial class SharePage : BaseContentPage
    {
        private SharePageViewModel _vm;

        public SharePage(string cipherId)
        {
            InitializeComponent();
            _vm = BindingContext as SharePageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
            else
            {
                _organizationPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }
            _organizationPicker.ItemDisplayBinding = new Binding("Key");
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync());
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
