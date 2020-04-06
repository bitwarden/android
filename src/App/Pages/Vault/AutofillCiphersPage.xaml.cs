using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AutofillCiphersPage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private readonly IPlatformUtilsService _platformUtilsService;

        private AutofillCiphersPageViewModel _vm;

        public AutofillCiphersPage(AppOptions appOptions)
        {
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as AutofillCiphersPageViewModel;
            _vm.Page = this;
            _vm.Init(appOptions);

            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, false, async () =>
            {
                try
                {
                    await _vm.LoadAsync();
                }
                catch (Exception e) when(e.Message.Contains("No key."))
                {
                    await Task.Delay(1000);
                    await _vm.LoadAsync();
                }
            }, _mainContent);
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }
            if (e.SelectedItem is GroupingsPageListItem item && item.Cipher != null)
            {
                await _vm.SelectCipherAsync(item.Cipher, item.FuzzyAutofill);
            }
        }

        private async void AddButton_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            if (_appOptions.FillType.HasValue && _appOptions.FillType != CipherType.Login)
            {
                var pageForOther = new AddEditPage(type: _appOptions.FillType, fromAutofill: true);
                await Navigation.PushModalAsync(new NavigationPage(pageForOther));
                return;
            }
            var pageForLogin = new AddEditPage(null, CipherType.Login, uri: _vm.Uri, name: _vm.Name,
                fromAutofill: true);
            await Navigation.PushModalAsync(new NavigationPage(pageForLogin));
        }

        private void Search_Clicked(object sender, System.EventArgs e)
        {
            var page = new CiphersPage(null, autofillUrl: _vm.Uri);
            Application.Current.MainPage = new NavigationPage(page);
        }
    }
}
