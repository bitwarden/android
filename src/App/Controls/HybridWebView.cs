using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class HybridWebView : View
    {
        private Action<string> _func;

        public static readonly BindableProperty UriProperty = BindableProperty.Create(propertyName: nameof(Uri),
            returnType: typeof(string), declaringType: typeof(HybridWebView), defaultValue: default(string));

        public string Uri
        {
            get { return (string)GetValue(UriProperty); }
            set { SetValue(UriProperty, value); }
        }

        public void RegisterAction(Action<string> callback)
        {
            _func = callback;
        }

        public void Cleanup()
        {
            _func = null;
        }

        public void InvokeAction(string data)
        {
            _func?.Invoke(data);
        }
    }
}
