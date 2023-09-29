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
                    effects.Add<Effects.NoEmojiKeyboardEffect, Bit.iOS.Core.Effects.NoEmojiKeyboardEffect>();
                    effects.Add<Effects.ScrollEnabledEffect, Effects.ScrollEnabledPlatformEffect>();
                    effects.Add<Effects.ScrollViewContentInsetAdjustmentBehaviorEffect, Bit.App.Effects.ScrollViewContentInsetAdjustmentBehaviorPlatformEffect>();
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
#else
                    iOS.Core.Handlers.ButtonHandlerMappings.Setup();
                    iOS.Core.Handlers.DatePickerHandlerMappings.Setup();
                    iOS.Core.Handlers.EditorHandlerMappings.Setup();
                    iOS.Core.Handlers.EntryHandlerMappings.Setup();
                    //iOS.Core.Handlers.LabelHandlerMappings.Setup();
                    iOS.Core.Handlers.PickerHandlerMappings.Setup();
                    iOS.Core.Handlers.SearchBarHandlerMappings.Setup();
                    iOS.Core.Handlers.StepperHandlerMappings.Setup();
                    iOS.Core.Handlers.TimePickerHandlerMappings.Setup();
#endif
                }
            ).Build();
        }
    }
}
