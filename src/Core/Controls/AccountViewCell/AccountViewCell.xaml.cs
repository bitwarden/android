using System.Windows.Input;

namespace Bit.App.Controls
{
    public partial class AccountViewCell : ViewCell
    {
        public static readonly BindableProperty SelectAccountCommandProperty = BindableProperty.Create(
            nameof(SelectAccountCommand), typeof(ICommand), typeof(AccountViewCell));

        public static readonly BindableProperty LongPressAccountCommandProperty = BindableProperty.Create(
            nameof(LongPressAccountCommand), typeof(ICommand), typeof(AccountViewCell));

        public AccountViewCell()
        {
            InitializeComponent();
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
    }
}
