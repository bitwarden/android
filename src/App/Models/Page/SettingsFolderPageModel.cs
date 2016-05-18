namespace Bit.App.Models.Page
{
    public class SettingsFolderPageModel
    {
        public SettingsFolderPageModel(Folder folder)
        {
            Id = folder.Id;
            Name = folder.Name?.Decrypt();
        }

        public string Id { get; set; }
        public string Name { get; set; }
    }
}
