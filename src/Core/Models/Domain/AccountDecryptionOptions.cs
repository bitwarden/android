using System;
namespace Bit.Core.Models.Domain
{
    public class AccountDecryptionOptions
    {
        public bool HasMasterPassword { get; set; }
        public TrustedDeviceOption TrustedDeviceOption { get; set; }
        public KeyConnectorOption KeyConnectorOption { get; set; }
    }

    public class TrustedDeviceOption
    {
        public bool HasAdminApproval { get; set; }
        public bool HasLoginApprovingDevice { get; }
        public bool HasManageResetPasswordPermission { get; }
        public string EncryptedPrivateKey { get; }
        public string EncryptedUserKey { get; }
    }

    public class KeyConnectorOption
    {
        public bool KeyConnectorUrl { get; set; }
    }
}

