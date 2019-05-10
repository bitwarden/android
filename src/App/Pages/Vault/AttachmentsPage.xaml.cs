using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AttachmentsPage : BaseContentPage
    {
        private AttachmentsPageViewModel _vm;

        public AttachmentsPage(string cipherId)
        {
            InitializeComponent();
            _vm = BindingContext as AttachmentsPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
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
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }
    }
}
