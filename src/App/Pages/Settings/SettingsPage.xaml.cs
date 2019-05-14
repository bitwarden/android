using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class SettingsPage : ContentPage
    {
        private SettingsPageViewModel _vm;

        public SettingsPage()
        {
            InitializeComponent();
            _vm = BindingContext as SettingsPageViewModel;
            _vm.Page = this;
        }
    }
}
