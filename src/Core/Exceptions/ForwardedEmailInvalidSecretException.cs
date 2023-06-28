using System;
namespace Bit.Core.Exceptions
{
    public class ForwardedEmailInvalidSecretException : Exception
    {
        public ForwardedEmailInvalidSecretException(Exception innerEx)
            : base("Invalid API Secret", innerEx)
        {
        }
    }
}
