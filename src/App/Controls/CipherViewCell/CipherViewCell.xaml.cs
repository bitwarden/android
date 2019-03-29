using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class CipherViewCell : ViewCell
    {
        public static readonly BindableProperty CipherProperty = BindableProperty.Create(
            nameof(Cipher), typeof(CipherView), typeof(CipherViewCell), default(CipherView), BindingMode.OneWay);

        private CipherViewCellViewModel _viewModel;

        public CipherViewCell()
        {
            InitializeComponent();
            _viewModel = _layout.BindingContext as CipherViewCellViewModel;
        }

        public CipherView Cipher
        {
            get => GetValue(CipherProperty) as CipherView;
            set => SetValue(CipherProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if(propertyName == CipherProperty.PropertyName)
            {
                _viewModel.Cipher = Cipher;
            }
        }
    }
}
