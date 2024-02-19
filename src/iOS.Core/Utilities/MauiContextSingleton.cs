namespace Bit.iOS.Core.Utilities
{
    public class MauiContextSingleton
    {
        private static readonly Lazy<MauiContextSingleton> _instance = new Lazy<MauiContextSingleton>(() => new MauiContextSingleton());

        public static MauiContextSingleton Instance => _instance.Value;

        private MauiContextSingleton() { }        

        public MauiContext? MauiContext { get; private set; }

        public void Init(MauiContext mauiContext) => MauiContext = mauiContext;
    }
}
