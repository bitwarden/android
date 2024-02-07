namespace Bit.Core.Utilities.Fido2
{
    public class Fido2ClientException : Exception
    {
        public enum ErrorCode 
        {
            NotAllowedError,
            TypeError,
            SecurityError,
            UnknownError
        }

        public readonly ErrorCode Code;
        public readonly string Reason;

        public Fido2ClientException(ErrorCode code, string reason) : base($"{code} ({reason})")
        {
            Code = code;
            Reason = reason;
        }
    }
}
