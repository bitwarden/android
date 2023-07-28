using System.Linq;
using CommunityToolkit.Maui.Converters;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class ExtendedCollectionView : CollectionView
    {
        public string ExtraDataForLogging { get; set; }
    }

    public class SelectionChangedEventArgsConverter : BaseNullableConverterOneWay<SelectionChangedEventArgs, object>
    {
        public override object? ConvertFrom(SelectionChangedEventArgs? value)
        {
            return value?.CurrentSelection.FirstOrDefault();
        }
    }

}
