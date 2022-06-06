using System.Collections.Generic;
using Bit.Core.Enums;

namespace Bit.Core.Models.View
{
    public abstract class ItemView : View
    {
        public ItemView() { }

        public abstract string SubTitle { get; }

        public abstract List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions { get; }
    }
}
