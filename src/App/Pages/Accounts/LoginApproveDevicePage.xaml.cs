using System;
using System.Collections.Generic;

using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginApproveDevicePage : BaseContentPage
    {

        private readonly LoginApproveDeviceViewModel _vm;
        public LoginApproveDevicePage()
        {
            InitializeComponent();
            _vm = BindingContext as LoginApproveDeviceViewModel;
            _vm.Page = this;
        }

        protected override void OnAppearing(){
            _vm.InitAsync();
        }
    }
}

