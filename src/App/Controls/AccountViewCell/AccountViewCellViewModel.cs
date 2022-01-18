using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class AccountViewCellViewModel : ExtendedViewModel
    {
        private AccountView _accountView;

        public AccountViewCellViewModel(AccountView accountView)
        {
            AccountView = accountView;
        }
        
        public AccountView AccountView
        {
            get => _accountView;
            set => SetProperty(ref _accountView, value);
        }

        public bool IsAccount
        {
            get => AccountView.IsAccount;
        }
        
        public string AuthStatusText
        {
            get => AccountView.AuthStatus.ToString();
        }
    }
}
