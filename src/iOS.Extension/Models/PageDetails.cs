using System;
using System.Collections.Generic;
using System.Linq;

namespace Bit.iOS.Extension.Models
{
    public class PageDetails
    {
        public string DocumentUUID { get; set; }
        public string Title { get; set; }
        public string Url { get; set; }
        public string DocumentUrl { get; set; }
        public string TabUrl { get; set; }
        public Dictionary<string, Form> Forms { get; set; }
        public List<Field> Fields { get; set; }
        public long CollectedTimestamp { get; set; }
        public bool HasPasswordField => Fields.Any(f => f.Type == "password");

        public class Form
        {
            public string OpId { get; set; }
            public string HtmlName { get; set; }
            public string HtmlId { get; set; }
            public string HtmlAction { get; set; }
            public string HtmlMethod { get; set; }
        }

        public class Field
        {
            public string OpId { get; set; }
            public int ElementNumber { get; set; }
            public bool Visible { get; set; }
            public bool Viewable { get; set; }
            public string HtmlId { get; set; }
            public string HtmlName { get; set; }
            public string HtmlClass { get; set; }
            public string LabelRight { get; set; }
            public string LabelLeft { get; set; }
            public string Type { get; set; }
            public string Value { get; set; }
            public bool Disabled { get; set; }
            public bool Readonly { get; set; }
            public string OnePasswordFieldType { get; set; }
            public string Form { get; set; }
        }
    }

}
