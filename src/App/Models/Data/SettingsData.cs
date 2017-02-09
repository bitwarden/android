using SQLite;
using Bit.App.Abstractions;

namespace Bit.App.Models.Data
{
    [Table("Settings")]
    public class SettingsData : IDataObject<string>
    {
        public SettingsData()
        { }

        [PrimaryKey]
        public string Id { get; set; }
        public string EquivalentDomains { get; set; }
    }
}
