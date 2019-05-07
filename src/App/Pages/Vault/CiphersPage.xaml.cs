using Bit.App.Resources;
using Bit.Core.Models.View;
using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class CiphersPage : BaseContentPage
    {
        private CiphersPageViewModel _vm;
        private bool _hasFocused;

        public CiphersPage(Func<CipherView, bool> filter, bool folder = false, bool collection = false,
            bool type = false)
        {
            InitializeComponent();
            _vm = BindingContext as CiphersPageViewModel;
            _vm.Page = this;
            _vm.Filter = filter;
            if(folder)
            {
                _vm.PageTitle = AppResources.SearchFolder;
            }
            else if(collection)
            {
                _vm.PageTitle = AppResources.SearchCollection;
            }
            else if(type)
            {
                _vm.PageTitle = AppResources.SearchType;
            }
            else
            {
                _vm.PageTitle = AppResources.SearchVault;
            }
        }

        public SearchBar SearchBar => _searchBar;

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(!_hasFocused)
            {
                _hasFocused = true;
                RequestFocus(_searchBar);
            }
        }

        private void SearchBar_TextChanged(object sender, TextChangedEventArgs e)
        {
            var oldLength = e.OldTextValue?.Length ?? 0;
            var newLength = e.NewTextValue?.Length ?? 0;
            if(oldLength < 2 && newLength < 2 && oldLength < newLength)
            {
                return;
            }
            _vm.Search(e.NewTextValue, 300);
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
            GoBack();
            return true;
        }

        private void GoBack()
        {
            Navigation.PopModalAsync(false);
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if(e.SelectedItem is CipherView cipher)
            {
                await _vm.SelectCipherAsync(cipher);
            }
        }
    }
}
