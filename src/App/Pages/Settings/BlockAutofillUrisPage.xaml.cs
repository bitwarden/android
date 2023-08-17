using System.Threading.Tasks;
using Bit.App.Styles;
using Bit.App.Utilities;
using Bit.Core.Utilities;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class BlockAutofillUrisPage : BaseContentPage, IThemeDirtablePage
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

            UpdatePlaceholder();
        }

        public override async Task UpdateOnThemeChanged()
        {
            await base.UpdateOnThemeChanged();

            UpdatePlaceholder();
        }

        private void UpdatePlaceholder()
        {
            MainThread.BeginInvokeOnMainThread(() =>
                _emptyUrisPlaceholder.Source = ImageSource.FromFile(ThemeManager.UsingLightTheme ? "empty_uris_placeholder" : "empty_uris_placeholder_dark"));
        }
    }
}
