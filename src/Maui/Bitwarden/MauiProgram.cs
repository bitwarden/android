using System;
using Bit.App.Controls;
using CommunityToolkit.Maui;
using FFImageLoading.Maui;
using Microsoft.Extensions.Logging;
using Microsoft.Maui.Controls.Compatibility.Hosting;
using Microsoft.Maui.Controls.Hosting;
using Microsoft.Maui.Hosting;
using SkiaSharp.Views.Maui.Controls.Hosting;
using ZXing.Net.Maui;
using ZXing.Net.Maui.Controls;

namespace Bit.App;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .UseMauiCommunityToolkit()
            .UseMauiCompatibility()
            .UseBarcodeReader()
            .UseSkiaSharp()
            .UseFFImageLoading()
            .ConfigureEffects(effects =>
            {
#if ANDROID
                effects.Add<Effects.FabShadowEffect, Effects.FabShadowPlatformEffect>();
                effects.Add<Effects.FixedSizeEffect, Effects.FixedSizePlatformEffect>();
                effects.Add<Effects.NoEmojiKeyboardEffect, Effects.NoEmojiKeyboardPlatformEffect>();
                effects.Add<Effects.TabBarEffect, Effects.TabBarPlatformEffect>();
                effects.Add<Effects.RemoveFontPaddingEffect, Effects.RemoveFontPaddingPlatformEffect>();
#endif
            })
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("RobotoMono_Regular.ttf#Roboto Mono", "RobotoMono_Regular");
                fonts.AddFont("bwi-font.ttf#bwi-font", "bwi-font");
                fonts.AddFont("MaterialIcons_Regular.ttf#Material Icons", "MaterialIcons_Regular");
            });
            // TODO: [MAUI-Migration] Convert renderers to handlers
            // Currently, there's an issue on reusing renderers https://github.com/dotnet/maui/issues/9936
//            .ConfigureMauiHandlers(handlers =>
//            {
//#if ANDROID
//                handlers.AddHandler(typeof(Editor), typeof(Droid.Renderers.CustomEditorRenderer));
//                handlers.AddHandler(typeof(Entry), typeof(Droid.Renderers.CustomEntryRenderer));
//                handlers.AddHandler(typeof(CustomLabel), typeof(Droid.Renderers.CustomLabelRenderer));
//                //handlers.AddHandler(typeof(ContentPage), typeof(Droid.Renderers.CustomPageRenderer));
//                handlers.AddHandler(typeof(Picker), typeof(Droid.Renderers.CustomPickerRenderer));
//                handlers.AddHandler(typeof(SearchBar), typeof(Droid.Renderers.CustomSearchBarRenderer));
//                handlers.AddHandler(typeof(Switch), typeof(Droid.Renderers.CustomSwitchRenderer));
//                handlers.AddHandler(typeof(TabbedPage), typeof(Droid.Renderers.CustomTabbedRenderer));
//                handlers.AddHandler(typeof(ExtendedDatePicker), typeof(Droid.Renderers.ExtendedDatePickerRenderer));
//                handlers.AddHandler(typeof(ExtendedGrid), typeof(Droid.Renderers.ExtendedGridRenderer));
//                handlers.AddHandler(typeof(ExtendedSlider), typeof(Droid.Renderers.ExtendedSliderRenderer));
//                handlers.AddHandler(typeof(ExtendedStackLayout), typeof(Droid.Renderers.ExtendedStackLayoutRenderer));
//                handlers.AddHandler(typeof(ExtendedStepper), typeof(Droid.Renderers.ExtendedStepperRenderer));
//                handlers.AddHandler(typeof(ExtendedTimePicker), typeof(Droid.Renderers.ExtendedTimePickerRenderer));
//                handlers.AddHandler(typeof(HybridWebView), typeof(Droid.Renderers.HybridWebViewRenderer));
//                handlers.AddHandler(typeof(SelectableLabel), typeof(Droid.Renderers.SelectableLabelRenderer));
//#elif IOS
//            // TODO: configure
//#endif
//            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}

