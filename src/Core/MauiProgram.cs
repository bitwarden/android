using Camera.MAUI;
using CommunityToolkit.Maui;
using FFImageLoading.Maui;
using Microsoft.Extensions.Logging;
using Microsoft.Maui.Controls.Compatibility.Hosting;
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
            .UseFFImageLoading()
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
                customHandlers?.Invoke(handlers);
            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder;
    }
}
