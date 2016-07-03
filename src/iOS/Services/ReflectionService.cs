using System;
using System.Reflection;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.iOS.Services
{
    public class ReflectionService : IReflectionService
    {
        public Func<double, double, SizeRequest> GetVisualElementOnSizeRequest(ExtendedTableView tableView)
        {
            var handle = typeof(VisualElement).GetMethod(
                "OnSizeRequest",
                BindingFlags.Instance | BindingFlags.NonPublic,
                null,
                new Type[] { typeof(double), typeof(double) },
                null)?.MethodHandle;

            if(!handle.HasValue)
            {
                throw new ArgumentNullException("handle could not be found.");
            }

            var pointer = handle.Value.GetFunctionPointer();
            if(pointer == null)
            {
                throw new ArgumentNullException("pointer could not be found.");
            }

            return (Func<double, double, SizeRequest>)Activator.CreateInstance(typeof(Func<double, double, SizeRequest>), tableView, pointer);
        }
    }
}
