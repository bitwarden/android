using Camera.MAUI;
using CommunityToolkit.Maui;
#if !UT
using FFImageLoading.Maui;
#endif
using Microsoft.Extensions.Logging;
using Microsoft.Maui.Controls.Compatibility.Hosting;
using Microsoft.Maui.Handlers;
using SkiaSharp.Views.Maui.Controls.Hosting;
using AppEffects = Bit.App.Effects;

namespace Bit.Core;

public static class MauiProgram
{
    public static MauiAppBuilder ConfigureMauiAppBuilder(Action<IEffectsBuilder> customEffectsBuilder, Action<IMauiHandlersCollection> customHandlers, bool initUseMauiApp = false)
    {
        var builder = MauiApp.CreateBuilder();
        if(initUseMauiApp)
        {
            builder.UseMauiApp<Bit.App.App>();
        }
        builder
            .UseMauiCommunityToolkit()
            .UseMauiCompatibility()
            .UseMauiCameraView()
            .UseSkiaSharp()
#if !UT
            .UseFFImageLoading()
#endif
            .ConfigureEffects(effects =>
            {
#if ANDROID
                effects.Add<AppEffects.FixedSizeEffect, AppEffects.FixedSizePlatformEffect>();
                effects.Add<AppEffects.NoEmojiKeyboardEffect, AppEffects.NoEmojiKeyboardPlatformEffect>();
                effects.Add<AppEffects.RemoveFontPaddingEffect, AppEffects.RemoveFontPaddingPlatformEffect>();
#endif
                customEffectsBuilder?.Invoke(effects);
            })
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("RobotoMono_Regular.ttf#Roboto Mono", "RobotoMono_Regular");
                fonts.AddFont("bwi-font.ttf#bwi-font", "bwi-font");
                fonts.AddFont("MaterialIcons_Regular.ttf#Material Icons", "MaterialIcons_Regular");
            })
            .ConfigureMauiHandlers(handlers =>
            {
#if ANDROID
                // HACK: Due to https://github.com/dotnet/maui/issues/19681 and not willing to use reflection to access
                // the alert dialog, we need to redefine the PickerHandler implementation for a custom one of ours
                // which handles showing the current selected item. Remove this workaround when MAUI releases a fix for this.
                if (handlers.FirstOrDefault(h => h.ServiceType == typeof(Picker)) is ServiceDescriptor sd)
                {
                    handlers.Remove(sd);
                    handlers.AddHandler(typeof(IPicker), typeof(Controls.Picker.PickerHandler));
                }
#endif
                customHandlers?.Invoke(handlers);
            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder;
    }
}
