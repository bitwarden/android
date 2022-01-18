using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class AccountViewCell : ViewCell
    {
        public static readonly BindableProperty AccountProperty = BindableProperty.Create(
            nameof(Account), typeof(AccountView), typeof(AccountViewCell), default(AccountView), BindingMode.OneWay);

        public AccountViewCell()
        {
            InitializeComponent();
        }

        public AccountView Account
        {
            get => GetValue(AccountProperty) as AccountView;
            set => SetValue(AccountProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == AccountProperty.PropertyName)
            {
                if (Account == null)
                {
                    return;
                }
                BindingContext = new AccountViewCellViewModel(Account);
            }
        }
    }
}
