using Bit.App.Models.Data;
using Bit.App.Models.Api;

namespace Bit.App.Models
{
    public class Folder : Cipher
    {
        public Folder()
        { }

        public Folder(FolderData data)
        {
            Id = data.Id;
            Name = data.Name != null ? new CipherString(data.Name) : null;
        }

        public Folder(FolderResponse response)
        {
            Id = response.Id;
            Name = response.Name != null ? new CipherString(response.Name) : null;
        }

        public FolderRequest ToFolderRequest()
        {
            return new FolderRequest(this);
        }

        public FolderData ToFolderData(string userId)
        {
            return new FolderData(this, userId);
        }
    }
}
