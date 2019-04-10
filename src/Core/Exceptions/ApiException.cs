using Bit.Core.Models.Response;
using System;

namespace Bit.Core.Exceptions
{
    public class ApiException : Exception
    {
        public ApiException(ErrorResponse error)
            : base("An API error has occurred.")
        { }

        public ErrorResponse Error { get; set; }
    }
}
