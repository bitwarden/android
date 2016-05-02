using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class ErrorResponse
    {
        public string Message { get; set; }
        public Dictionary<string, IEnumerable<string>> ValidationErrors { get; set; }
        // For use in development environments.
        public string ExceptionMessage { get; set; }
        public string ExceptionStackTrace { get; set; }
        public string InnerExceptionMessage { get; set; }
    }
}
