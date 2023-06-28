using System;
using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class DeviceResponse
    {
        public string Id { get; set; }
        public string UserId { get; set; }
        public string Name { get; set; }
        public string Identifier { get; set; }
        public DeviceType Type { get; set; }
        public string CreationDate { get; set; }
        public string RevisionDate { get; set; }
    }
}

