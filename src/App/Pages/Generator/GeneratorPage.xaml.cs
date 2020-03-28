using Bit.App.Resources;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;

namespace Bit.App.Pages
{
    public partial class GeneratorPage : BaseContentPage
    {
        private GeneratorPageViewModel _vm;
        private readonly bool _fromTabPage;
        private readonly Action<string> _selectAction;
        private readonly TabsPage _tabsPage;

        public GeneratorPage(bool fromTabPage, Action<string> selectAction = null, TabsPage tabsPage = null)
        {
            _tabsPage = tabsPage;
            InitializeComponent();
            _vm = BindingContext as GeneratorPageViewModel;
            _vm.Page = this;
            _fromTabPage = fromTabPage;
            _selectAction = selectAction;
            var isIos = Device.RuntimePlatform == Device.iOS;
            if (selectAction != null)
            {
                if (isIos)
                {
                    ToolbarItems.Add(_closeItem);
                }
                ToolbarItems.Add(_selectItem);
            }
            else
            {
                if (isIos)
                {
                    ToolbarItems.Add(_moreItem);
                }
                else
                {
                    ToolbarItems.Add(_historyItem);
                }
            }
            if (isIos)
            {
                _typePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }
        }

        public async Task InitAsync()
        {
            await _vm.InitAsync();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            if (!_fromTabPage)
            {
                await InitAsync();
            }
        }

        protected override bool OnBackButtonPressed()
        {
            if (Device.RuntimePlatform == Device.Android && _tabsPage != null)
            {
                _tabsPage.ResetToVaultPage();
                return true;
            }
            return base.OnBackButtonPressed();
        }

        private async void Regenerate_Clicked(object sender, EventArgs e)
        {
            await _vm.RegenerateAsync();
        }

        private async void Copy_Clicked(object sender, EventArgs e)
        {
            await _vm.CopyAsync();
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                null, AppResources.PasswordHistory);
            if (selection == AppResources.PasswordHistory)
            {
                var page = new GeneratorHistoryPage();
                await Navigation.PushModalAsync(new Xamarin.Forms.NavigationPage(page));
            }
        }

        private void Select_Clicked(object sender, EventArgs e)
        {
            _selectAction?.Invoke(_vm.Password);
        }

        private async void History_Clicked(object sender, EventArgs e)
        {
            var page = new GeneratorHistoryPage();
            await Navigation.PushModalAsync(new Xamarin.Forms.NavigationPage(page));
        }

        private async void LengthSlider_DragCompleted(object sender, EventArgs e)
        {
            await _vm.SliderChangedAsync();
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
