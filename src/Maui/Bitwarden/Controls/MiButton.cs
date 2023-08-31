namespace Bit.App.Controls
{
    public class MiButton : Button
    {
        public MiButton()
        {
            Padding = 0;
#if ANDROID
            FontFamily = "MaterialIcons_Regular";
#else
            FontFamily = "Material Icons";
#endif
        }
    }
}
