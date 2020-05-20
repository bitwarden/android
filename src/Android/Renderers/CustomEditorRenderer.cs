using Android.Content;
using Android.Views.InputMethods;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Editor), typeof(CustomEditorRenderer))]
namespace Bit.Droid.Renderers
{
    public class CustomEditorRenderer : EditorRenderer
    {
        public CustomEditorRenderer(Context context)
            : base(context)
        { }
        
        // Workaround for issue described here:
        // https://github.com/xamarin/Xamarin.Forms/issues/8291#issuecomment-617456651
        protected override void OnAttachedToWindow()
        {
            base.OnAttachedToWindow();
            EditText.Enabled = false;
            EditText.Enabled = true;
        }

        protected override void OnElementChanged(ElementChangedEventArgs<Editor> e)
        {
            base.OnElementChanged(e);
            if (Control != null && e.NewElement != null)
            {
                Control.SetPadding(Control.PaddingLeft, Control.PaddingTop - 10, Control.PaddingRight,
                    Control.PaddingBottom + 20);
                Control.ImeOptions = Control.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                    (ImeAction)ImeFlags.NoExtractUi;
            }
        }
    }
}
