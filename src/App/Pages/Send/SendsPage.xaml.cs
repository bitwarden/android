using System;
using System.Linq;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SendsPage : BaseContentPage
    {
        private SendsPageViewModel _vm;
        private bool _hasFocused;

        public SendsPage(Func<SendView, bool> filter, bool type = false)
        {
            InitializeComponent();
            _vm = BindingContext as SendsPageViewModel;
            _vm.Page = this;
            _vm.Filter = filter;
            if (type)
            {
                _vm.PageTitle = AppResources.SearchType;
            }
            else
            {
                _vm.PageTitle = AppResources.SearchSends;
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
            GoBack();
            return true;
        }

        private void GoBack()
        {
            if (!DoOnce())
            {
                return;
            }
            Navigation.PopModalAsync(false);
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }

            if (e.CurrentSelection?.FirstOrDefault() is SendView send)
            {
                await _vm.SelectSendAsync(send);
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            GoBack();
        }
    }
}
