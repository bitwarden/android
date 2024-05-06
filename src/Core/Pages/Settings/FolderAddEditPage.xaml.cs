using System.Collections.Generic;
using Bit.Core.Resources.Localization;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class FolderAddEditPage : BaseContentPage
    {
        private FolderAddEditPageViewModel _vm;

        public FolderAddEditPage(
            string folderId = null)
        {
            InitializeComponent();
            _vm = BindingContext as FolderAddEditPageViewModel;
            _vm.Page = this;
            _vm.FolderId = folderId;
            _vm.Init();
            SetActivityIndicator();
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (!_vm.EditMode || Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Remove(_deleteItem);
            }
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (_vm.EditMode && Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_moreItem);
            }
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, async () =>
            {
                await _vm.LoadAsync();
                if (!_vm.EditMode)
                {
                    RequestFocus(_nameEntry);
                }
            });
        }

        private async void Save_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Delete_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.DeleteAsync();
            }
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            var options = new List<string> { };
            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                _vm.EditMode ? AppResources.Delete : null, options.ToArray());
            if (selection == AppResources.Delete)
            {
                await _vm.DeleteAsync();
            }
        }
    }
}
