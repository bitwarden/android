using System;
namespace Bit.Core.Models.Domain
{
    public class AccountDecryptionOptions
    {
        public bool HasMasterPassword { get; set; }
        public TrustedDeviceOption TrustedDeviceOption { get; set; }
        public KeyConnectorOption KeyConnectorOption { get; set; }

        public bool RequireSetPassword => !HasMasterPassword && KeyConnectorOption == null;
    }

    public class TrustedDeviceOption
    {
        public bool HasAdminApproval { get; set; }
        public bool HasLoginApprovingDevice { get; set; }
        public bool HasManageResetPasswordPermission { get; set; }
        public string EncryptedPrivateKey { get; set; }
        public string EncryptedUserKey { get; set; }
    }

    public class KeyConnectorOption
    {
        public string KeyConnectorUrl { get; set; }
    }
}

