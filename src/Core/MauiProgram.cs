using CommunityToolkit.Maui;
using FFImageLoading.Maui;
using Microsoft.Extensions.Logging;
using Microsoft.Maui.Controls.Compatibility.Hosting;
using SkiaSharp.Views.Maui.Controls.Hosting;
using ZXing.Net.Maui.Controls;
using AppEffects = Bit.App.Effects;

namespace Bit.Core;

public static class MauiProgram
{
    public static MauiAppBuilder ConfigureMauiAppBuilder(Action<IEffectsBuilder> customEffectsBuilder, Action<IMauiHandlersCollection> customHandlers)
    {
        return ConfigureBaseMauiAppBuilder(customEffectsBuilder, customHandlers)
            .UseMauiApp<Bit.App.App>();
    }
    public static MauiAppBuilder ConfigureBaseMauiAppBuilder(Action<IEffectsBuilder> customEffectsBuilder, Action<IMauiHandlersCollection> customHandlers)
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiCommunityToolkit()
            .UseMauiCompatibility()
            .UseBarcodeReader()
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
