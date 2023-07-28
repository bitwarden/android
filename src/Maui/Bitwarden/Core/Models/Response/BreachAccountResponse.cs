using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class BreachAccountResponse
    {
        public string AddedDate { get; set; }
        public string BreachDate { get; set; }
        public List<string> DataClasses { get; set; }
        public string Description { get; set; }
        public string Domain { get; set; }
        public bool IsActive { get; set; }
        public bool IsVerified { get; set; }
        public string LogoPath { get; set; }
        public string ModifiedDate { get; set; }
        public string Name { get; set; }
        public int PwnCount { get; set; }
        public string Title { get; set; }
    }
}
