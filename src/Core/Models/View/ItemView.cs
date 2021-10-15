using System.Collections.Generic;

namespace Bit.Core.Models.View
{
    public abstract class ItemView : View
    {
        public ItemView() { }

        public abstract string SubTitle { get; }

        // Must return the same id values as LinkedMetadata decorators in jslib
        public abstract List<KeyValuePair<string, int>> LinkedMetadata { get; }
    }
}
