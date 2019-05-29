using Bit.App.Resources;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class SettingsPage : BaseContentPage
    {
        private SettingsPageViewModel _vm;

        public SettingsPage()
        {
            InitializeComponent();
            _vm = BindingContext as SettingsPageViewModel;
            _vm.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private async void RowSelected(object sender, SelectedItemChangedEventArgs e)
        {
            ((ListView)sender).SelectedItem = null;
            if(!DoOnce())
            {
                return;
            }
            if(!(e.SelectedItem is SettingsPageListItem item))
            {
                return;
            }

            if(item.Name == AppResources.Sync)
            {
                await Navigation.PushModalAsync(new NavigationPage(new SyncPage()));
            }
            else if(item.Name == AppResources.Options)
            {
                await Navigation.PushModalAsync(new NavigationPage(new OptionsPage()));
            }
            else if(item.Name == AppResources.Folders)
            {
                await Navigation.PushModalAsync(new NavigationPage(new FoldersPage()));
            }
            else if(item.Name == AppResources.About)
            {
                await _vm.AboutAsync();
            }
            else if(item.Name == AppResources.HelpAndFeedback)
            {
                _vm.Help();
            }
            else if(item.Name == AppResources.FingerprintPhrase)
            {
                await _vm.FingerprintAsync();
            }
            else if(item.Name == AppResources.RateTheApp)
            {
                _vm.Rate();
            }
            else if(item.Name == AppResources.ImportItems)
            {
                _vm.Import();
            }
            else if(item.Name == AppResources.ExportVault)
            {
                _vm.Export();
            }
            else if(item.Name == AppResources.ShareVault)
            {
                await _vm.ShareAsync();
            }
            else if(item.Name == AppResources.WebVault)
            {
                _vm.WebVault();
            }
            else if(item.Name == AppResources.ChangeMasterPassword)
            {
                await _vm.ChangePasswordAsync();
            }
            else if(item.Name == AppResources.TwoStepLogin)
            {
                await _vm.TwoStepAsync();
            }
            else if(item.Name == AppResources.LogOut)
            {
                await _vm.LogOutAsync();
            }
            else if(item.Name == AppResources.LockNow)
            {
                await _vm.LockAsync();
            }
            else if(item.Name == AppResources.LockOptions)
            {
                await _vm.LockOptionsAsync();
            }
            else if(item.Name == AppResources.UnlockWithPIN)
            {
                await _vm.UpdatePinAsync();
            }
            else if(item.Name.Contains(AppResources.Fingerprint) || item.Name.Contains(AppResources.TouchID) ||
                item.Name.Contains(AppResources.FaceID))
            {
                await _vm.UpdateFingerprintAsync();
            }
        }
    }
}
