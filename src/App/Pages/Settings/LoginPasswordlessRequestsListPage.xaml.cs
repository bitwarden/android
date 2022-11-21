using System;
using System.Collections.Generic;
using System.Linq;
using Bit.Core.Abstractions;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessRequestsListPage : BaseContentPage
    {
        private LoginPasswordlessRequestsListViewModel _vm;

        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public LoginPasswordlessRequestsListPage()
        {
            InitializeComponent();
            _vm = BindingContext as LoginPasswordlessRequestsListViewModel;
            _vm.Page = this;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _vm.RefreshCommand.Execute(null);
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            try
            {
                if (!(e.CurrentSelection?.FirstOrDefault() is PasswordlessLoginResponse item))
                {
                    return;
                }

                _vm.AnswerRequestCommand.ExecuteAsync(item);
            }
            catch (Exception ex)
            {
                _logger?.Value.Exception(ex);
            }
        }
    }
}

