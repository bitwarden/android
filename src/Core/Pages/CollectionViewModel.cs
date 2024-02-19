using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class CollectionViewModel : ExtendedViewModel
    {
        private bool _checked;

        public Core.Models.View.CollectionView Collection { get; set; }
        public bool Checked
        {
            get => _checked;
            set => SetProperty(ref _checked, value);
        }
    }
}
