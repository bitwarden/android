using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using System.Linq;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class CiphersPage : BaseContentPage
    {
        private readonly string _autofillUrl;
        private readonly IDeviceActionService _deviceActionService;

        private CiphersPageViewModel _vm;
        private bool _hasFocused;

        public CiphersPage(Func<CipherView, bool> filter, bool folder = false, bool collection = false,
            bool type = false, string autofillUrl = null, bool deleted = false)
        {
            InitializeComponent();
            _vm = BindingContext as CiphersPageViewModel;
            _vm.Page = this;
            _vm.Filter = filter;
            _vm.AutofillUrl = _autofillUrl = autofillUrl;
            _vm.Deleted = deleted;
            if (deleted)
            {
                _vm.PageTitle = AppResources.SearchTrash;
            }
            else if (folder)
            {
                _vm.PageTitle = AppResources.SearchFolder;
            }
            else if (collection)
            {
                _vm.PageTitle = AppResources.SearchCollection;
            }
            else if (type)
            {
                _vm.PageTitle = AppResources.SearchType;
            }
            else
            {
                _vm.PageTitle = AppResources.SearchVault;
            }

            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_closeItem);
                _searchBar.Placeholder = AppResources.Search;
                _mainLayout.Children.Insert(0, _searchBar);
                _mainLayout.Children.Insert(1, _separator);
            }
            else
            {
                NavigationPage.SetTitleView(this, _titleLayout);
            }
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
        }

        public SearchBar SearchBar => _searchBar;

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if (!_hasFocused)
            {
                _hasFocused = true;
                RequestFocus(_searchBar);
            }
        }

        private void SearchBar_TextChanged(object sender, TextChangedEventArgs e)
        {
            var oldLength = e.OldTextValue?.Length ?? 0;
            var newLength = e.NewTextValue?.Length ?? 0;
            if (oldLength < 2 && newLength < 2 && oldLength < newLength)
            {
                return;
            }
            _vm.Search(e.NewTextValue, 200);
        }

        private void SearchBar_SearchButtonPressed(object sender, EventArgs e)
        {
            _vm.Search((sender as SearchBar).Text);
        }

        private void BackButton_Clicked(object sender, EventArgs e)
        {
            GoBack();
        }

        protected override bool OnBackButtonPressed()
        {
            if (string.IsNullOrWhiteSpace(_autofillUrl))
            {
                return false;
            }
            GoBack();
            return true;
        }

        private void GoBack()
        {
            if (!DoOnce())
            {
                return;
            }
            if (string.IsNullOrWhiteSpace(_autofillUrl))
            {
                Navigation.PopModalAsync(false);
            }
            else
            {
                _deviceActionService.CloseAutofill();
            }
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }

            if (e.CurrentSelection?.FirstOrDefault() is CipherView cipher)
            {
                await _vm.SelectCipherAsync(cipher);
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            GoBack();
        }
    }
}
