using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class GroupingsPage : ContentPage
    {
        private GroupingsPageViewModel _viewModel;

        public GroupingsPage()
        {
            InitializeComponent();
            _viewModel = BindingContext as GroupingsPageViewModel;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _viewModel.LoadAsync();
        }
    }
}
