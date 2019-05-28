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
            if(!_vm.EditMode)
            {
                ToolbarItems.Remove(_deleteItem);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, async () =>
            {
                await _vm.LoadAsync();
                if(!_vm.EditMode)
                {
                    RequestFocus(_nameEntry);
                }
            });
        }

        private async void Save_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Delete_Clicked(object sender, System.EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.DeleteAsync();
            }
        }
    }
}
