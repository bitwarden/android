using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Card
    {
        public Card()
        {
            CardholderName = "John Doe";
            Brand = "visa";
            Number = "4242424242424242";
            ExpMonth = "04";
            ExpYear = "2023";
            Code = "123";
        }

        public static CardView ToView(Card req, CardView view = null)
        {
            if(view == null)
            {
                view = new CardView();
            }

            view.CardholderName = req.CardholderName;
            view.Brand = req.Brand;
            view.Number = req.Number;
            view.ExpMonth = req.ExpMonth;
            view.ExpYear = req.ExpYear;
            view.Code = req.Code;
            return view;
        }

        public string CardholderName { get; set; }
        public string Brand { get; set; }
        public string Number { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }

        public Card(CardView obj)
        {
            if(obj == null)
            {
                return;
            }

            CardholderName = obj.CardholderName;
            Brand = obj.Brand;
            Number = obj.Number;
            ExpMonth = obj.ExpMonth;
            ExpYear = obj.ExpYear;
            Code = obj.Code;
        }
    }
}
