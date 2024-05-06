namespace Bit.Core.Exceptions
{
    public class ValidationException : Exception
    {
        public ValidationException(string localizedMessage)
            : base(localizedMessage)
        {
        }
    }
}
