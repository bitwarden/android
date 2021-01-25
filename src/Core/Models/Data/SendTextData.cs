using System.Drawing;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class SendTextData : Data
    {
        public SendTextData() { }

        public SendTextData(SendTextApi data)
        {
            Text = data.Text;
            Hidden = data.Hidden;
        }

        public string Text { get; set; }
        public bool Hidden { get; set; }
    }
}
