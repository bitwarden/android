namespace Bit.App
{
    public class MauiProgram
    {
        public static MauiApp CreateMauiApp()
        {
            return Core.MauiProgram.ConfigureMauiAppBuilder(
                effects =>
                {
#if ANDROID
                    effects.Add<Effects.FabShadowEffect, Effects.FabShadowPlatformEffect>();
#else
                    iOS.Core.Utilities.iOSCoreHelpers.ConfigureMAUIEffects(effects);
#endif
                },
                handlers =>
                {
#if ANDROID
                    Bit.App.Handlers.EntryHandlerMappings.Setup();
                    Bit.App.Handlers.EditorHandlerMappings.Setup();
                    Bit.App.Handlers.LabelHandlerMappings.Setup();
                    Bit.App.Handlers.PickerHandlerMappings.Setup();
                    Bit.App.Handlers.SearchBarHandlerMappings.Setup();
                    Bit.App.Handlers.SwitchHandlerMappings.Setup();
                    Bit.App.Handlers.DatePickerHandlerMappings.Setup();
                    Bit.App.Handlers.SliderHandlerMappings.Setup();
                    Bit.App.Handlers.StepperHandlerMappings.Setup();
                    Bit.App.Handlers.TimePickerHandlerMappings.Setup();
                    Bit.App.Handlers.ButtonHandlerMappings.Setup();

                    handlers.AddHandler(typeof(TabbedPage), typeof(Bit.App.Handlers.CustomTabbedPageHandler));
#else
                    iOS.Core.Utilities.iOSCoreHelpers.ConfigureMAUIHandlers(handlers);
#endif
                },
               initUseMauiApp: true
            ).Build();
        }
    }
}
