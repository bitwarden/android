using System;
using System.Collections.Generic;
using Bit.App.Models;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginApproveDevicePage : BaseContentPage
    {

        private readonly LoginApproveDeviceViewModel _vm;
        public LoginApproveDevicePage(AppOptions appOptions = null)
        {
            InitializeComponent();
            _vm = BindingContext as LoginApproveDeviceViewModel;
            _vm.Page = this;
        }

        protected override void OnAppearing(){
            _vm.InitAsync();
        }

        private void Cancel_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }
    }
}

