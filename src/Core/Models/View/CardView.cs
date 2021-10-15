using Bit.Core.Models.Domain;
using System.Collections.Generic;
using System.Text.RegularExpressions;

namespace Bit.Core.Models.View
{
    public class CardView : ItemView
    {
        private string _brand;
        private string _number;
        private string _subTitle;

        public CardView() { }

        public CardView(Card c) { }

        public string CardholderName { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }
        public string MaskedCode => Code != null ? new string('•', Code.Length) : null;
        public string MaskedNumber => Number != null ? new string('•', Number.Length) : null;

        public string Brand
        {
            get => _brand;
            set
            {
                _brand = value;
                _subTitle = null;
            }
        }

        public string Number
        {
            get => _number;
            set
            {
                _number = value;
                _subTitle = null;
            }
        }

        public override string SubTitle
        {
            get
            {
                if (_subTitle == null)
                {
                    _subTitle = Brand;
                    if (Number != null && Number.Length >= 4)
                    {
                        if (!string.IsNullOrWhiteSpace(_subTitle))
                        {
                            _subTitle += ", ";
                        }
                        else
                        {
                            _subTitle = string.Empty;
                        }
                        // Show last 5 on amex, last 4 for all others
                        var count = Number.Length >= 5 && Regex.Match(Number, "^3[47]").Success ? 5 : 4;
                        _subTitle += ("*" + Number.Substring(Number.Length - count));
                    }
                }
                return _subTitle;
            }
        }

        public string Expiration
        {
            get
            {
                var expMonthNull = string.IsNullOrWhiteSpace(ExpMonth);
                var expYearNull = string.IsNullOrWhiteSpace(ExpYear);
                if (expMonthNull && expYearNull)
                {
                    return null;
                }
                var expMo = !expMonthNull ? ExpMonth.PadLeft(2, '0') : "__";
                var expYr = !expYearNull ? FormatYear(ExpYear) : "____";
                return string.Format("{0} / {1}", expMo, expYr);
            }
        }

        public override List<KeyValuePair<string, int>> LinkedMetadata
        {
            get => new List<KeyValuePair<string, int>>()
            {
                new KeyValuePair<string, int>("CardholderName", 0),
                new KeyValuePair<string, int>("ExpirationMonth", 1),
                new KeyValuePair<string, int>("ExpirationYear", 2),
                new KeyValuePair<string, int>("SecurityCode", 3),
                new KeyValuePair<string, int>("Brand", 4),
                new KeyValuePair<string, int>("Number", 5),
            };
        }

        private string FormatYear(string year)
        {
            return year.Length == 2 ? string.Concat("20", year) : year;
        }
    }
}
