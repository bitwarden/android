using Bit.Core.Models.Domain;
using System;

namespace Bit.Core.Models.View
{
    public class PasswordHistoryView : View
    {
        public PasswordHistoryView() { }

        public PasswordHistoryView(PasswordHistory ph)
        {
            LastUsedDate = ph.LastUsedDate;
        }
        
        public string Password { get; set; }
        public DateTime? LastUsedDate { get; set; }
    }
}
