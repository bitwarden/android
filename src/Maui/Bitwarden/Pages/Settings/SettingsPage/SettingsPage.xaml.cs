using System;
using System.ComponentModel;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Controls;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class SettingsPage : BaseContentPage
    {
        private readonly TabsPage _tabsPage;
        private SettingsPageViewModel _vm;

        public SettingsPage(TabsPage tabsPage)
        {
            _tabsPage = tabsPage;
            InitializeComponent();
            _vm = BindingContext as SettingsPageViewModel;
            _vm.Page = this;
        }

        public async Task InitAsync()
        {
            await _vm.InitAsync();
        }

        public void BuildList()
        {
            _vm.BuildList();
        }

        protected override bool OnBackButtonPressed()
        {
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android && _tabsPage != null)
            {
                _tabsPage.ResetToVaultPage();
                return true;
            }
            return base.OnBackButtonPressed();
        }

        void ActivateTimePicker(object sender, EventArgs args)
        {
            var stackLayout = (ExtendedStackLayout)sender;
            SettingsPageListItem item = (SettingsPageListItem)stackLayout.BindingContext;
            if (item.ShowTimeInput)
            {
                var timePicker = stackLayout.Children.Where(x => x is TimePicker).FirstOrDefault();
                ((TimePicker)timePicker)?.Focus();
            }
        }

        async void OnTimePickerPropertyChanged(object sender, PropertyChangedEventArgs args)
        {
            var s = (TimePicker)sender;
            var time = s.Time.TotalMinutes;
            if (s.IsFocused && args.PropertyName == "Time")
            {
                await _vm.VaultTimeoutAsync(false, (int)time);
            }
        }

        private void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (e.CurrentSelection?.FirstOrDefault() is SettingsPageListItem item)
            {
                _vm?.ExecuteSettingItemCommand.Execute(item);
            }
        }
    }
}
