using System;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Behaviors
{
    public class ConnectivityBehavior : Behavior<Element>
    {
        private readonly IConnectivity _connectivity;

        public ConnectivityBehavior()
        {
            _connectivity = Resolver.Resolve<IConnectivity>();
        }

        private static readonly BindablePropertyKey IsValidPropertyKey = BindableProperty.CreateReadOnly("Connected", typeof(bool), typeof(ConnectivityBehavior), false);
        public static readonly BindableProperty IsValidProperty = IsValidPropertyKey.BindableProperty;

        public bool Connected
        {
            get { return (bool)GetValue(IsValidProperty); }
            private set { SetValue(IsValidPropertyKey, value); }
        }

        protected override void OnAttachedTo(Element el)
        {
            _connectivity.ConnectivityChanged += ConnectivityChanged;
            base.OnAttachedTo(el);
        }

        private void ConnectivityChanged(object sender, ConnectivityChangedEventArgs e)
        {
            Connected = e.IsConnected;
        }

        protected override void OnDetachingFrom(Element el)
        {
            _connectivity.ConnectivityChanged -= ConnectivityChanged;
            base.OnDetachingFrom(el);
        }
    }
}
