namespace Bit.App.Handlers
{
    public partial class StepperHandlerMappings
    {
        public void Setup() => SetupPlatform();

        partial void SetupPlatform();
    }
}

