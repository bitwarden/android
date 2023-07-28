using System;

namespace Bit.Core.Models.Request
{
    public class PasswordHistoryRequest
    {
        public string Password { get; set; }
        public DateTime? LastUsedDate { get; set; }
    }
}
