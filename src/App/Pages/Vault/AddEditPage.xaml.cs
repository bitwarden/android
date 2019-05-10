using Bit.Core.Enums;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class AddEditPage : BaseContentPage
    {
        private AddEditPageViewModel _vm;

        public AddEditPage(
            string cipherId = null,
            CipherType? type = null,
            string folderId = null,
            string collectionId = null)
        {
            InitializeComponent();
            _vm = BindingContext as AddEditPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            _vm.FolderId = folderId;
            _vm.CollectionIds = collectionId != null ? new HashSet<string>(new List<string> { collectionId }) : null;
            _vm.Type = type;
            _vm.Init();
            SetActivityIndicator();

            _typePicker.ItemDisplayBinding = new Binding("Key");
            _cardBrandPicker.ItemDisplayBinding = new Binding("Key");
            _cardExpMonthPicker.ItemDisplayBinding = new Binding("Key");
            _identityTitlePicker.ItemDisplayBinding = new Binding("Key");
            _folderPicker.ItemDisplayBinding = new Binding("Key");
            _ownershipPicker.ItemDisplayBinding = new Binding("Key");
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync());
            if(Device.RuntimePlatform == Device.Android)
            {
                if(_vm.Cipher.OrganizationId == null)
                {
                    if(ToolbarItems.Contains(_collectionsItem))
                    {
                        ToolbarItems.Remove(_collectionsItem);
                    }
                    if(!ToolbarItems.Contains(_shareItem))
                    {
                        ToolbarItems.Insert(2, _shareItem);
                    }
                }
                else
                {
                    if(ToolbarItems.Contains(_shareItem))
                    {
                        ToolbarItems.Remove(_shareItem);
                    }
                    if(!ToolbarItems.Contains(_collectionsItem))
                    {
                        ToolbarItems.Insert(2, _collectionsItem);
                    }
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
        }

        private async void PasswordHistory_Tapped(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await Navigation.PushModalAsync(new NavigationPage(new PasswordHistoryPage(_vm.CipherId)));
            }
        }

        private async void Save_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private void NewUri_Clicked(object sender, System.EventArgs e)
        {
            _vm.AddUri();
        }

        private void NewField_Clicked(object sender, System.EventArgs e)
        {
            _vm.AddField();
        }

        private void Attachments_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                // await Navigation.PushModalAsync();
            }
        }

        private async void Share_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                var page = new SharePage(_vm.CipherId);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private async void Delete_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.DeleteAsync();
            }
        }

        private void Collections_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                // TODO
            }
        }
    }
}
