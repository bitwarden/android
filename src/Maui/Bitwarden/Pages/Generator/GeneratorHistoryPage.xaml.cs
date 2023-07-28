using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Styles;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class GeneratorHistoryPage : BaseContentPage, IThemeDirtablePage
    {
        private GeneratorHistoryPageViewModel _vm;

        public GeneratorHistoryPage()
        {
            InitializeComponent();
            SetActivityIndicator();
            _vm = BindingContext as GeneratorHistoryPageViewModel;
            _vm.Page = this;
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_closeItem);
                ToolbarItems.Add(_moreItem);
            }
            else
            {
                ToolbarItems.Add(_clearItem);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();

            await LoadOnAppearedAsync(_mainLayout, true, async () =>
            {
                await _vm.InitAsync();
            });
        }

        private async void Clear_Clicked(object sender, EventArgs e)
        {
            await _vm.ClearAsync();
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                null, AppResources.Clear);
            if (selection == AppResources.Clear)
            {
                await _vm.ClearAsync();
            }
        }

        public override async Task UpdateOnThemeChanged()
        {
            await base.UpdateOnThemeChanged();

            await _vm?.UpdateOnThemeChanged();
        }
    }
}
