using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class ApiResult<T>
    {
        private List<ApiError> m_errors = new List<ApiError>();

        public bool Succeeded { get; private set; }
        public T Result { get; set; }
        public IEnumerable<ApiError> Errors => m_errors;

        public static ApiResult<T> Success(T result)
        {
            return new ApiResult<T>
            {
                Succeeded = true,
                Result = result
            };
        }

        public static ApiResult<T> Failed(params ApiError[] errors)
        {
            var result = new ApiResult<T> { Succeeded = false };
            if(errors != null)
            {
                result.m_errors.AddRange(errors);
            }

            return result;
        }
    }
}
