using System;
using SQLite;
using Bit.App.Abstractions;

namespace Bit.App.Models.Data
{
    [Table("Folder")]
    public class FolderData : IDataObject<int>
    {
        public FolderData()
        { }

        public FolderData(Folder folder)
        {
            Id = folder.Id;
            ServerId = folder.ServerId;
            Name = folder.Name?.EncryptedString;
        }

        [PrimaryKey]
        [AutoIncrement]
        public int Id { get; set; }
        public string ServerId { get; set; }
        public string Name { get; set; }
        public DateTime RevisionDateTime { get; set; } = DateTime.UtcNow;

        public Folder ToFolder()
        {
            return new Folder(this);
        }
    }
}
