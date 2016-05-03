namespace Bit.App.Models.Api
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
