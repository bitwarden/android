using System;

namespace Bit.App.Pages
{
    public partial class GeneratorPage : BaseContentPage
    {
        private GeneratorPageViewModel _vm;
        private readonly Action<string> _selectAction;

        public GeneratorPage()
            : this(null)
        { }

        public GeneratorPage(Action<string> selectAction)
        {
            InitializeComponent();
            _vm = BindingContext as GeneratorPageViewModel;
            _vm.Page = this;
            _selectAction = selectAction;
            if(selectAction == null)
            {
                ToolbarItems.Remove(_selectItem);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private async void Regenerate_Clicked(object sender, EventArgs e)
        {
            await _vm.RegenerateAsync();
        }

        private async void Copy_Clicked(object sender, EventArgs e)
        {
            await _vm.CopyAsync();
        }

        private void Select_Clicked(object sender, EventArgs e)
        {
            _selectAction?.Invoke(_vm.Password);
        }

        private void History_Clicked(object sender, EventArgs e)
        {

        }
    }
}
