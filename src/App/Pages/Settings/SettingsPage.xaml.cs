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

        private void RowSelected(object sender, SelectedItemChangedEventArgs e)
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

            // TODO
        }
    }
}
