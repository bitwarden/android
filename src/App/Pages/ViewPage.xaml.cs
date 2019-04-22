using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class ViewPage : ContentPage
    {
        private ViewPageViewModel _vm;

        public ViewPage()
        {
            InitializeComponent();
            _vm = BindingContext as ViewPageViewModel;
            _vm.Page = this;
        }
    }
}
