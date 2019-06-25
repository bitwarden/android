using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.Request
{
    public class EventRequest
    {
        public EventType Type { get; set; }
        public string CipherId { get; set; }
    }
}
