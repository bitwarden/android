using Bit.App.Enums;
using System;

namespace Bit.App.Models.Api
{
    public class DeviceResponse
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Identifier { get; set; }
        public DeviceType Type { get; set; }
        public DateTime CreationDate { get; set; }
    }
}
