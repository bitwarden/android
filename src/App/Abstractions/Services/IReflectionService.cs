using System;
using Bit.App.Controls;
using Xamarin.Forms;

namespace Bit.App.Abstractions
{
    public interface IReflectionService
    {
        Func<double, double, SizeRequest> GetVisualElementOnSizeRequest(ExtendedTableView tableView);
    }
}
