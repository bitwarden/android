using Bit.Core.Models.Domain;

namespace Bit.Core.Models.Request
{
    public class FolderRequest
    {
        public FolderRequest(Folder folder)
        {
            Name = folder.Name?.EncryptedString;
        }

        public string Name { get; set; }
    }
}
