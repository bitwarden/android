using SQLite;

namespace Bit.App.Models.Data
{
    [Table("CipherCollection")]
    public class CipherCollectionData
    {
        [PrimaryKey]
        [AutoIncrement]
        public int Id { get; set; }
        [Indexed]
        public string UserId { get; set; }
        [Indexed]
        public string CipherId { get; set; }
        [Indexed]
        public string CollectionId { get; set; }
    }
}
