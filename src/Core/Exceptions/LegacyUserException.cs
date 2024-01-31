using System;
namespace Bit.Core.Exceptions
{
    public class LegacyUserException : Exception
    {
        public LegacyUserException()
            : base("Legacy users must migrate on web vault.")
        {
        }
    }
}
