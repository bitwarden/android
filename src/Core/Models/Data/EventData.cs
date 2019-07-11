using Bit.Core.Enums;
using System;

namespace Bit.Core.Models.Data
{
    public class EventData : Data
    {
        public EventType Type { get; set; }
        public string CipherId { get; set; }
        public DateTime Date { get; set; }
    }
}
