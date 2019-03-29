using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class VaultGroupingsPage : ContentPage
    {
        private VaultGroupingsPageViewModel _viewModel;

        public VaultGroupingsPage()
        {
            InitializeComponent();
            _viewModel = BindingContext as VaultGroupingsPageViewModel;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _viewModel.LoadAsync();
        }
    }

    public class ListItemDataTemplateSelector : DataTemplateSelector
    {
        public DataTemplate CipherTemplate { get; set; }
        public DataTemplate FolderTemplate { get; set; }
        public DataTemplate CollectionTemplate { get; set; }

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            if(item is VaultGroupingsPageListItem listItem)
            {
                if(listItem.Collection != null)
                {
                    return CollectionTemplate;
                }
                else if(listItem.Folder != null)
                {
                    return FolderTemplate;
                }
                return CipherTemplate;
            }
            return null;
        }
    }
}
