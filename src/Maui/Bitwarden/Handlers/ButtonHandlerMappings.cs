namespace Bit.App.Handlers
{
    public partial class ButtonHandlerMappings
    {
        public void Setup() => SetupPlatform();

        partial void SetupPlatform();
    }
}
