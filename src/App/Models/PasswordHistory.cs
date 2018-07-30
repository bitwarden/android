using Bit.App.Models.Data;

namespace Bit.App.Models
{
    public class PasswordHistory
    {
        public PasswordHistory() { }

        public PasswordHistory(PasswordHistoryDataModel model)
        {
            Password = model.Password != null ? new CipherString(model.Password) : null;
            LastUsedDate = model.LastUsedDate;
        }
        
        public CipherString Password { get; set; }
        public System.DateTime LastUsedDate { get; set; }
    }
}
