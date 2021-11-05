using System;
using System.Collections.Generic;

using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ConvertMasterPasswordPage : BaseContentPage
    {
        private readonly ConvertMasterPasswordPageViewModel _vm;
        public ConvertMasterPasswordPage()
        {
            InitializeComponent();
            _vm = BindingContext as ConvertMasterPasswordPageViewModel;
        }

        private async void Continue_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void LeaveOrg_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
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
