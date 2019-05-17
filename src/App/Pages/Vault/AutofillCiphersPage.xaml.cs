using Bit.App.Models;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AutofillCiphersPage : BaseContentPage
    {
        private AutofillCiphersPageViewModel _vm;
        private readonly AppOptions _appOptions;

        public AutofillCiphersPage(AppOptions appOptions)
        {
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as AutofillCiphersPageViewModel;
            _vm.Page = this;
            _vm.Init(appOptions);
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, false, async () =>
            {
                await _vm.LoadAsync();
            }, _mainContent);
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if(!DoOnce())
            {
                return;
            }

            if(e.SelectedItem is GroupingsPageListItem item && item.Cipher != null)
            {
                // TODO
            }
        }

        private void AddButton_Clicked(object sender, System.EventArgs e)
        {

        }

        private void Search_Clicked(object sender, System.EventArgs e)
        {

        }
    }
}
