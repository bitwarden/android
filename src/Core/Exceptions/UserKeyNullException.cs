using System;
namespace Bit.Core.Exceptions
{
    public class UserKeyNullException : Exception
    {
        public UserKeyNullException()
            : base("UserKey is null.")
        {
        }
    }
}

