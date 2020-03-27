using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class CollectionsPage : BaseContentPage
    {
        private CollectionsPageViewModel _vm;

        public CollectionsPage(string cipherId)
        {
            InitializeComponent();
            _vm = BindingContext as CollectionsPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync());
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
        }

        private async void Save_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
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
