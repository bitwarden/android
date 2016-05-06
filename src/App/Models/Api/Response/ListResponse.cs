using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class ListResponse<T>
    {
        public ListResponse(IEnumerable<T> data)
        {
            Data = data;
        }

        public IEnumerable<T> Data { get; set; }
    }
}
