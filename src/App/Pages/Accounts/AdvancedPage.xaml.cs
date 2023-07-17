using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Essentials;
using Xamarin.Forms;


namespace Bit.App.Pages
{
    public partial class AdvancedPage : BaseContentPage
    {
        private readonly AdvancedPageViewModel _vm;

        public AdvancedPage()
        {
            InitializeComponent();
            _vm = BindingContext as AdvancedPageViewModel;
            _vm.Page = this;
            _vm.OkAction = () => Device.BeginInvokeOnMainThread(async () => await Navigation.PopModalAsync());
        }
    }
}
