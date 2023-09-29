using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class AccountViewCellViewModel : ExtendedViewModel
    {
        private AccountView _accountView;
        private AvatarImageSource _avatar;

        public AccountViewCellViewModel(AccountView accountView)
        {
            AccountView = accountView;
            AvatarImageSource = ServiceContainer.Resolve<IAvatarImageSourcePool>("avatarImageSourcePool")
                ?.GetOrCreateAvatar(AccountView.UserId, AccountView.Name, AccountView.Email, AccountView.AvatarColor);
        }

        public AccountView AccountView
        {
            get => _accountView;
            set => SetProperty(ref _accountView, value);
        }

        public AvatarImageSource AvatarImageSource
        {
            get => _avatar;
            set => SetProperty(ref _avatar, value);
        }

        public bool IsAccount
        {
            get => AccountView.IsAccount;
        }

        public bool ShowHostname
        {
            get => !string.IsNullOrWhiteSpace(AccountView.Hostname);
        }

        public bool IsActive
        {
            get => AccountView.IsActive;
        }

        public bool IsUnlocked
        {
            get => AccountView.AuthStatus == AuthenticationStatus.Unlocked;
        }

        public bool IsUnlockedAndNotActive
        {
            get => IsUnlocked && !IsActive;
        }

        public bool IsLocked
        {
            get => AccountView.AuthStatus == AuthenticationStatus.Locked;
        }

        public bool IsLockedAndNotActive
        {
            get => IsLocked && !IsActive;
        }

        public bool IsLoggedOut
        {
            get => AccountView.AuthStatus == AuthenticationStatus.LoggedOut;
        }

        public bool IsLoggedOutAndNotActive
        {
            get => IsLoggedOut && !IsActive;
        }

        public string AuthStatusIconActive
        {
            get => BitwardenIcons.CheckCircle;
        }

        public string AuthStatusIconNotActive
        {
            get
            {
                if (IsUnlocked)
                {
                    return BitwardenIcons.Unlock;
                }
                return BitwardenIcons.Lock;
            }
        }
    }
}
