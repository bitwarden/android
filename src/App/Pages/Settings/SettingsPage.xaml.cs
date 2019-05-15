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
            else if(item.Name == AppResources.Folders)
            {
                await Navigation.PushModalAsync(new NavigationPage(new FoldersPage()));
            }
            else if(item.Name == AppResources.About)
            {
                await _vm.AboutAsync();
            }
        }
    }
}
