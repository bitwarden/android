using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities.Automation;

namespace Bit.App.Pages
{
    public class SettingsPageListItem
    {
        private readonly string _nameResourceKey;

        public SettingsPageListItem(string nameResourceKey, Func<Task> executeAsync)
        {
            _nameResourceKey = nameResourceKey;
            ExecuteAsync = executeAsync;
        }

        public string Name => AppResources.ResourceManager.GetString(_nameResourceKey);

        public Func<Task> ExecuteAsync { get; }

        public string AutomationId
        {
            get
            {
                return AutomationIdsHelper.AddSuffixFor(AutomationIdsHelper.ToEnglishTitleCase(_nameResourceKey), SuffixType.Cell);
            }
        }
    }
}
