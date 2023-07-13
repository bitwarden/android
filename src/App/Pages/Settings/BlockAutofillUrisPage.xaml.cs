using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class BlockAutofillUrisPage : BaseContentPage
    {
        private readonly BlockAutofillUrisPageViewModel _vm;

        public BlockAutofillUrisPage()
        {
            InitializeComponent();

            _vm = BindingContext as BlockAutofillUrisPageViewModel;
            _vm.Page = this;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            _vm.InitAsync().FireAndForget(_ => Navigation.PopAsync());
        }
    }
}
