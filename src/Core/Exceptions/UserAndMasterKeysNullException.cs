using System;
namespace Bit.Core.Exceptions
{
    public class UserAndMasterKeysNullException : Exception
    {
        public UserAndMasterKeysNullException()
            : base("UserKey and MasterKey are null.")
        {
        }
    }
}

