using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class AccountViewCellViewModel : ExtendedViewModel
    {
        private AccountView _account;

        public AccountViewCellViewModel(AccountView accountView)
        {
            Account = accountView;
        }
        
        public AccountView Account
        {
            get => _account;
            set => SetProperty(ref _account, value);
        }

        public bool IsAccount
        {
            get => Account.IsAccount;
        }
        
        public string AuthStatusText
        {
            get => Account.AuthStatus.ToString();
        }
    }
}
