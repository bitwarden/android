using System.Collections.Generic;
using System.Net;

namespace Bit.App.Models.Api
{
    public class ApiResult<T>
    {
        private List<ApiError> m_errors = new List<ApiError>();

        public bool Succeeded { get; private set; }
        public T Result { get; set; }
        public IEnumerable<ApiError> Errors => m_errors;
        public HttpStatusCode StatusCode { get; private set; }

        public static ApiResult<T> Success(T result, HttpStatusCode statusCode)
        {
            return new ApiResult<T>
            {
                Succeeded = true,
                Result = result,
                StatusCode = statusCode
            };
        }

        public static ApiResult<T> Failed(HttpStatusCode statusCode, params ApiError[] errors)
        {
            var result = new ApiResult<T>
            {
                Succeeded = false,
                StatusCode = statusCode
            };

            if(errors != null)
            {
                result.m_errors.AddRange(errors);
            }

            return result;
        }
    }

    public class ApiResult
    {
        private List<ApiError> m_errors = new List<ApiError>();

        public bool Succeeded { get; private set; }
        public IEnumerable<ApiError> Errors => m_errors;
        public HttpStatusCode StatusCode { get; private set; }

        public static ApiResult Success(HttpStatusCode statusCode)
        {
            return new ApiResult
            {
                Succeeded = true,
                StatusCode = statusCode
            };
        }

        public static ApiResult Failed(HttpStatusCode statusCode, params ApiError[] errors)
        {
            var result = new ApiResult
            {
                Succeeded = false,
                StatusCode = statusCode
            };

            if(errors != null)
            {
                result.m_errors.AddRange(errors);
            }

            return result;
        }
    }
}
