using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.App.Abstractions
{
    public interface ISteamGuardService
    { 
        string TOTPSecret { get; }
        string RecoveryCode { get; }
        void Init(string username, string password);
        bool CheckEmailCode(string code);
        bool CheckSMSCode(string code);
    }
}
