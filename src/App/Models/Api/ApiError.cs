using System.Net;

namespace Bit.App.Models.Api
{
    public class ApiError
    {
        public string Message { get; set; }
        public HttpStatusCode StatusCode { get; set; }
    }
}
