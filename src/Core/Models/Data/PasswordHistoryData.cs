using System;
using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class PasswordHistoryData : Data
    {
        public PasswordHistoryData() { }

        public PasswordHistoryData(PasswordHistoryResponse data)
        {
            Password = data.Password;
            LastUsedDate = data.LastUsedDate;
        }

        public string Password { get; set; }
        public DateTime? LastUsedDate { get; set; }
    }
}
