namespace Bit.App.Controls
{
#if !UT
    public class CachedImage : FFImageLoading.Maui.CachedImage
    {
    }
#else
    /// <summary>
    /// Given that FFImageLoading package doesn't support net8.0 then for Unit tests projects to build and run correctly
    /// we need to not include the reference to FFImageLoading and therefore wrap this class
    /// to provide a stub one that does nothing so this project doesn't break and we can run the tests.
    /// </summary>
    public class CachedImage : View
    {
        public static readonly BindableProperty SourceProperty = BindableProperty.Create(
            nameof(Source), typeof(ImageSource), typeof(CachedImage));

        public static readonly BindableProperty AspectProperty = BindableProperty.Create(
            nameof(Aspect), typeof(Aspect), typeof(CachedImage));

        public bool BitmapOptimizations { get; set; }
        public string ErrorPlaceholder { get; set; }
        public string LoadingPlaceholder { get; set; }

        public ImageSource Source
        {
            get { return (ImageSource)GetValue(SourceProperty); }
            set { SetValue(SourceProperty, value); }
        }
        public Aspect Aspect
        {
            get { return (Aspect)GetValue(AspectProperty); }
            set { SetValue(AspectProperty, value); }
        }
    }
#endif
}
