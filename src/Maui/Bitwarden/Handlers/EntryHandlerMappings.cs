namespace Bit.App.Handlers
{
    public partial class EntryHandlerMappings
    {
        public void Setup() => SetupPlatform();

        partial void SetupPlatform();
    }
}
