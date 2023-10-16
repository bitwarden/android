namespace Bit.App
{
    public class MauiProgram
    {
        public static MauiApp CreateMauiApp()
        {
            return Core.MauiProgram.ConfigureMauiAppBuilder(
                effects =>
                {
#if IOS
                    iOS.Core.Utilities.iOSCoreHelpers.ConfigureMAUIEffects(effects);
#endif
                },
                handlers =>
                {
                    handlers.AddHandler(typeof(Bit.App.Controls.HybridWebView), typeof(Bit.App.Handlers.HybridWebViewHandler));
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
                    Bit.App.Handlers.ToolbarHandlerMappings.Setup();

                    handlers.AddHandler(typeof(Bit.App.Pages.TabsPage), typeof(Bit.App.Handlers.CustomTabbedPageHandler));
#else
                    iOS.Core.Utilities.iOSCoreHelpers.ConfigureMAUIHandlers(handlers);
#endif
                },
               initUseMauiApp: true
            ).Build();
        }
    }
}
