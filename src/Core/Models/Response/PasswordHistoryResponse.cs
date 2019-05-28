using System;

namespace Bit.Core.Models.Response
{
    public class PasswordHistoryResponse
    {
        public string Password { get; set; }
        public DateTime? LastUsedDate { get; set; }
    }
}
