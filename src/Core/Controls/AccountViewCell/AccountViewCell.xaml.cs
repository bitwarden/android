using System.Windows.Input;
using Bit.Core.Models.View;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public partial class AccountViewCell : ViewCell
    {
        public static readonly BindableProperty AccountProperty = BindableProperty.Create(
            nameof(Account), typeof(AccountView), typeof(AccountViewCell));

        public static readonly BindableProperty SelectAccountCommandProperty = BindableProperty.Create(
            nameof(SelectAccountCommand), typeof(ICommand), typeof(AccountViewCell));

        public static readonly BindableProperty LongPressAccountCommandProperty = BindableProperty.Create(
            nameof(LongPressAccountCommand), typeof(ICommand), typeof(AccountViewCell));

        public AccountViewCell()
        {
            InitializeComponent();
        }

        public AccountView Account
        {
            get => GetValue(AccountProperty) as AccountView;
            set => SetValue(AccountProperty, value);
        }

        public ICommand SelectAccountCommand
        {
            get => GetValue(SelectAccountCommandProperty) as ICommand;
            set => SetValue(SelectAccountCommandProperty, value);
        }

        public ICommand LongPressAccountCommand
        {
            get => GetValue(LongPressAccountCommandProperty) as ICommand;
            set => SetValue(LongPressAccountCommandProperty, value);
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
