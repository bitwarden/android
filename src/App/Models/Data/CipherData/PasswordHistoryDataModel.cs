using Bit.App.Models.Api;

namespace Bit.App.Models.Data
{
    public class PasswordHistoryDataModel
    {
        public PasswordHistoryDataModel() { }

        public PasswordHistoryDataModel(PasswordHistoryResponse h)
        {
            Password = h.Password;
            LastUsedDate = h.LastUsedDate;
        }
        
        public string Password { get; set; }
        public System.DateTime LastUsedDate { get; set; }
    }
}
