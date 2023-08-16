using System;
namespace Bit.Core.Exceptions
{
    public class MasterKeyNullException : Exception
    {
        public MasterKeyNullException()
            : base("MasterKey is null.")
        {
        }
    }
}

