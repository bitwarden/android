using System;
using System.Reflection;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.UWP.Services
{
    public class ReflectionService : IReflectionService
    {
        public Func<double, double, SizeRequest> GetVisualElementOnSizeRequest(ExtendedTableView tableView)
        { 

            var method = typeof(VisualElement).GetMethod(
                "OnSizeRequest",
                BindingFlags.Instance | BindingFlags.NonPublic
                );

            return (Func<double, double, SizeRequest>)Activator.CreateInstance(typeof(Func<double, double, SizeRequest>), tableView, method);
        }
    }
}
