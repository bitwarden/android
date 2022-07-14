using System;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Services;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Utilities
{
    public static class ImageSourceExtensions
    {
        /// <summary>
        /// Gets the native image from the ImageSource.
        /// Taken from https://github.com/xamarin/Xamarin.Forms/blob/02dee20dfa1365d0104758e534581d1fa5958990/Xamarin.Forms.Platform.iOS/Renderers/ImageElementManager.cs#L264
        /// </summary>
        public static async Task<UIImage> GetNativeImageAsync(this ImageSource source, CancellationToken cancellationToken = default(CancellationToken))
        {
            if (source == null || source.IsEmpty)
            {
                return null;
            }

            var handler = Xamarin.Forms.Internals.Registrar.Registered.GetHandlerForObject<IImageSourceHandler>(source);
            if (handler == null)
            {
                LoggerHelper.LogEvenIfCantBeResolved(new InvalidOperationException("GetNativeImageAsync failed cause IImageSourceHandler couldn't be found"));
                return null;
            }

            try
            {
                float scale = (float)UIScreen.MainScreen.Scale;
                return await handler.LoadImageAsync(source, scale: scale, cancelationToken: cancellationToken);
            }
            catch (OperationCanceledException)
            {
                LoggerHelper.LogEvenIfCantBeResolved(new OperationCanceledException("GetNativeImageAsync was cancelled"));
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(new InvalidOperationException("GetNativeImageAsync failed", ex));
            }

            return null;
        }
    }
}
